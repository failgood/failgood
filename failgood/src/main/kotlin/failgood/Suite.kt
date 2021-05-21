package failgood

import failgood.internal.*
import kotlinx.coroutines.*
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
                            throw FailGoodException("context ${context.name} timed out")
                        }
                    } else
                        ContextInfo(emptyList(), mapOf())
                }

            }
        }
    }

    private fun findRootContexts(coroutineScope: CoroutineScope) = contextProviders
        .map { coroutineScope.async { it.getContexts() } }

    fun rs(test: String): TestResult {
        val contextName = test.substringBefore(">").trim()
        val context = contextProviders.flatMap { it.getContexts() }.single {
            it.name == contextName
        }
        val desc = ContextPath.fromString(test)
        return runBlocking {
            val resourcesCloser = ResourcesCloser(this)
            SingleTestExecutor(context, desc, object : TestDSL, ResourcesDSL by resourcesCloser {
                override suspend fun println(body: String) {
                    kotlin.io.println(body)
                }
            }, resourcesCloser).execute()
        }
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
