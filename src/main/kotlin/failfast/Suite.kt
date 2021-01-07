package failfast

import failfast.internal.ContextExecutor
import failfast.internal.ContextInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    fun run(parallelism: Int = cpus()): SuiteResult {
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
    return "total:${uptime}ms cpu:${cpuTime}ms, load:${percentage}%"
}

private fun cpus() = Runtime.getRuntime().availableProcessors()
