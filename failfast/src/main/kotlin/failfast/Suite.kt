package failfast

import failfast.internal.ContextExecutor
import failfast.internal.ContextInfo
import failfast.internal.ContextTreeReporter
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

class Suite(val rootContexts: Collection<ContextProvider>) {
    companion object {
        fun fromContexts(rootContexts: Collection<RootContext>) =
            Suite(rootContexts.map { ContextProvider { it } })

        fun fromClasses(classes: List<KClass<*>>) =
            Suite(classes.map { ObjectContextProvider(it) })
    }

    init {
        if (rootContexts.isEmpty()) throw EmptySuiteException()
    }

    constructor(rootContext: RootContext) :
            this(listOf(ContextProvider { rootContext }))

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
        return rootContexts
            .map {
                coroutineScope.async {
                    val context = it.getContext()
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

    fun runSingle(test: String) {
        val contextName = test.substringBefore(">").trim()
        val context = rootContexts.map { it.getContext() }.single {
            it.name == contextName
        }
        val desc = ContextPath.fromString(test)
        val result = runBlocking {
            SingleTestExecutor(context, desc).execute()
        }
        if (result is Failed) {
            println(result.prettyPrint())
        } else
            println("${result.test} OK")

    }
}

object NullExecutionListener : ExecutionListener {
    override suspend fun testStarted(testDescriptor: TestDescription) {}
    override suspend fun testFinished(testResult: TestPlusResult) {}
}

internal fun uptime(): String {
    val operatingSystemMXBean =
        ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
    val uptime = ManagementFactory.getRuntimeMXBean().uptime
    val cpuTime = operatingSystemMXBean.processCpuTime / 1000000
    val percentage = cpuTime * 100 / uptime
    return "total:${uptime}ms cpu:${cpuTime}ms, load:${percentage}%"
}

private fun cpus() = Runtime.getRuntime().availableProcessors()
