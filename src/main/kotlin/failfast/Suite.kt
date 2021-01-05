package failfast

import failfast.internal.ContextExecutor
import failfast.internal.ContextInfo
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class Suite(val rootContexts: Collection<ContextProvider>, private val parallelism: Int = cpus()) {
    companion object {
        fun fromContexts(rootContexts: Collection<RootContext>, parallelism: Int = cpus()) =
            Suite(rootContexts.map { ContextProvider { it } }, parallelism)

        fun fromClasses(classes: List<Class<*>>, parallelism: Int = cpus()) =
            Suite(classes.map { ObjectContextProvider(it) }, parallelism)
    }

    init {
        if (rootContexts.isEmpty()) throw EmptySuiteException()
    }

    constructor(rootContext: RootContext, parallelism: Int = cpus()) :
        this(listOf(ContextProvider { rootContext }), parallelism)

    constructor(parallelism: Int = cpus(), function: ContextLambda) :
        this(RootContext("root", false, function), parallelism)

    fun run(): SuiteResult {
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

                        val results = contextInfos.flatMap { it.tests.values }.awaitAll()
                        SuiteResult(results, results.filterIsInstance<Failed>(), contextInfos.flatMap { it.contexts })
                    }
                }
        } finally {
            threadPool.awaitTermination(100, TimeUnit.SECONDS)
            threadPool.shutdown()
        }
    }

    internal suspend fun findTests(coroutineScope: CoroutineScope, executeTests: Boolean = true):
        List<ContextInfo> {
            val coroutineStart = if (executeTests) CoroutineStart.DEFAULT else CoroutineStart.LAZY
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
                                        coroutineStart = coroutineStart
                                    ).execute()
                                }
                            } catch (e: TimeoutCancellationException) {
                                throw FailFastException("context ${context.name} timed out")
                            }
                        } else
                            ContextInfo(emptySet(), mapOf())
                    }
                }
                .awaitAll()
        }
}

internal fun uptime(): String {
    val operatingSystemMXBean =
        ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
    val uptime = ManagementFactory.getRuntimeMXBean().uptime
    val cpuTime = operatingSystemMXBean.processCpuTime / 1000000
    val percentage = cpuTime * 100 / uptime
    return "total:${uptime}ms cpu:${cpuTime}ms, pct:${percentage}%"
}

private fun cpus() = Runtime.getRuntime().availableProcessors()
