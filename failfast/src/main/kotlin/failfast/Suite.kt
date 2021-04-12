package failfast

import failfast.internal.ContextExecutor
import failfast.internal.ContextInfo
import failfast.internal.ContextTreeReporter
import failfast.internal.ExceptionPrettyPrinter
import failfast.internal.SingleTestExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class Suite(val contextProviders: Collection<ContextProvider>) {
    companion object {
        fun fromContexts(rootContexts: Collection<RootContext>) =
            Suite(rootContexts.map { ContextProvider { listOf(it) } })

        fun fromClasses(classes: List<KClass<*>>) =
            Suite(classes.map { ObjectContextProvider(it) })
    }

    init {
        if (contextProviders.isEmpty()) throw EmptySuiteException()
    }

    constructor(rootContext: RootContext) :
            this(listOf(ContextProvider { listOf(rootContext) }))

    constructor(function: ContextLambda) :
            this(RootContext("root", false, function))

    fun run(
        parallelism: Int = cpus(),
        silent: Boolean = false
    ): SuiteResult {

        val threadPool =
            if (parallelism > 1)
                Executors.newWorkStealingPool(parallelism)
            else
                Executors.newSingleThreadExecutor()
        return try {
            threadPool.asCoroutineDispatcher()
                .use { dispatcher ->
                    runBlocking(dispatcher) {
                        val contextInfos = findTests(this)
                        if (!silent) {
                            contextInfos.forEach {
                                launch {
                                    val context = it.await()
                                    val contextTreeReporter = ContextTreeReporter()
                                    println(
                                        contextTreeReporter.stringReport(
                                            context.tests.values.awaitAll(),
                                            context.contexts
                                        )
                                            .joinToString("\n")
                                    )
                                }
                            }
                        }
                        val resolvedContexts = contextInfos.awaitAll()
                        val results = resolvedContexts.flatMap { it.tests.values }.awaitAll()
                        SuiteResult(
                            results,
                            results.filter { it.result is Failed },
                            resolvedContexts.flatMap { it.contexts })
                    }
                }
        } finally {
            threadPool.awaitTermination(100, TimeUnit.SECONDS)
            threadPool.shutdown()
        }
    }

    internal suspend fun findTests(
        coroutineScope: CoroutineScope,
        executeTests: Boolean = true,
        listener: ExecutionListener = NullExecutionListener
    ):
            List<Deferred<ContextInfo>> {
        return findRootContexts(coroutineScope).flatMap { deferredContexts: Deferred<List<RootContext>> ->
            val contexts = deferredContexts.await()
            contexts.map { context ->
                coroutineScope.async {
                    if (!context.disabled) {
                        try {
                            withTimeout(20000) {
                                ContextExecutor(
                                    context,
                                    coroutineScope,
                                    !executeTests,
                                    listener
                                ).execute()
                            }
                        } catch (e: TimeoutCancellationException) {
                            throw FailFastException("context ${context.name} timed out")
                        }
                    } else
                        ContextInfo(emptyList(), mapOf())
                }

            }
        }
    }

    private fun findRootContexts(coroutineScope: CoroutineScope) = contextProviders
        .map { coroutineScope.async { it.getContexts() } }

    fun runSingle(test: String) {
        val contextName = test.substringBefore(">").trim()
        val context = contextProviders.flatMap { it.getContexts() }.single {
            it.name == contextName
        }
        val desc = ContextPath.fromString(test)
        val result = runBlocking {
            SingleTestExecutor(context, desc, object : TestDSL {
                override suspend fun println(body: String) {
                    println(body)
                }
            }).execute()
        }
        if (result is Failed) {
            println("$test${ExceptionPrettyPrinter(result.failure).prettyPrint()}")
        } else
            println("$test OK")

    }
}

object NullExecutionListener : ExecutionListener {
    override suspend fun testStarted(testDescription: TestDescription) {}
    override suspend fun testFinished(testPlusResult: TestPlusResult) {}
    override suspend fun testEvent(testDescription: TestDescription, type: String, payload: String) {}
}

internal fun uptime(totalTests: Int? = null): String {
    val operatingSystemMXBean =
        ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
    val uptime = ManagementFactory.getRuntimeMXBean().uptime
    val cpuTime = operatingSystemMXBean.processCpuTime / 1000000
    val percentage = cpuTime * 100 / uptime
    return "total:${uptime}ms cpu:${cpuTime}ms, load:${percentage}%." + if (totalTests != null) {
        " " + pluralize(totalTests * 1000 / uptime.toInt(), "test") + "/sec"
    } else
        ""
}

internal fun pluralize(count: Int, item: String) = if (count == 1) "1 $item" else "$count ${item}s"
private fun cpus() = Runtime.getRuntime().availableProcessors()
