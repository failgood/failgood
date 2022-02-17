package failgood

import failgood.internal.ContextExecutor
import failgood.internal.ContextInfo
import failgood.internal.ContextResult
import failgood.internal.ContextTreeReporter
import failgood.internal.ExecuteAllTestFilterProvider
import failgood.internal.FailedContext
import failgood.internal.TestFilterProvider
import kotlinx.coroutines.*
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

const val DEFAULT_TIMEOUT: Long = 40000

class Suite(val contextProviders: Collection<ContextProvider>) {
    init {
        if (contextProviders.isEmpty()) throw EmptySuiteException()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun run(parallelism: Int = cpus(), silent: Boolean = false): SuiteResult {
        val threadPool = if (parallelism > 1)
            Executors.newWorkStealingPool(parallelism)
        else
            Executors.newSingleThreadExecutor()
        return try {
            threadPool.asCoroutineDispatcher()
                .use { dispatcher ->
                    runBlocking(dispatcher) {
                        val contextInfos = findTests(this)
                        if (!silent) {
                            printResults(this, contextInfos)
                        }
                        awaitTestResult(contextInfos)
                    }
                }
        } finally {
            threadPool.awaitTermination(100, TimeUnit.SECONDS)
            threadPool.shutdown()
        }
    }

    // set timeout to the timeout in milliseconds, an empty string to turn it off
    private val timeoutMillis: Long = System.getenv("TIMEOUT").let {
        when (it) {
            null -> DEFAULT_TIMEOUT
            "" -> null
            else -> it.toLongOrNull() ?: throw FailGoodException("TIMEOUT must be a number or an empty string")
        }
    } ?: Long.MAX_VALUE

    internal suspend fun findTests(
        coroutineScope: CoroutineScope,
        executeTests: Boolean = true,
        executionFilter: TestFilterProvider = ExecuteAllTestFilterProvider,
        listener: ExecutionListener = NullExecutionListener
    ): List<Deferred<ContextResult>> {
        return contextProviders
            .map {
                coroutineScope.async {
                    try {
                        it.getContexts()
                    } catch (e: ErrorLoadingContextsFromClass) {
                        listOf(CouldNotLoadContext(e, e.jClass))
                    }
                }
            }.flatMap { it.await() }.sortedBy { it.order }
            .map { context: LoadResult ->
                when (context) {
                    is CouldNotLoadContext ->
                        CompletableDeferred(
                            FailedContext(Context(context.jClass.name ?: "unknown"), context.reason)
                        )
                    is RootContext -> {
                        val testFilter = executionFilter.forClass(context.sourceInfo.className)
                        coroutineScope.async {
                            if (!context.disabled) {
                                ContextExecutor(
                                    context,
                                    coroutineScope,
                                    !executeTests,
                                    listener,
                                    testFilter,
                                    timeoutMillis
                                ).execute()
                            } else
                                ContextInfo(emptyList(), mapOf(), setOf())
                        }
                    }
                }
            }
    }
}

object NullExecutionListener : ExecutionListener {
    override suspend fun testStarted(testDescription: TestDescription) {}
    override suspend fun testFinished(testPlusResult: TestPlusResult) {}
    override suspend fun testEvent(testDescription: TestDescription, type: String, payload: String) {}
}

private val operatingSystemMXBean =
    ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
private val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
internal fun upt(): Long = runtimeMXBean.uptime

internal fun uptime(totalTests: Int? = null): String {
    val uptime = upt()
    val cpuTime = operatingSystemMXBean.processCpuTime / 1000000
    val percentage = cpuTime * 100 / uptime
    return "total:${uptime}ms cpu:${cpuTime}ms, load:$percentage%." + if (totalTests != null) {
        " " + pluralize(totalTests * 1000 / uptime.toInt(), "test") + "/sec"
    } else
        ""
}

private suspend fun awaitTestResult(
    contextInfos: List<Deferred<ContextResult>>
): SuiteResult {
    return awaitContexts(contextInfos.awaitAll())
}

internal suspend fun awaitContexts(resolvedContexts: List<ContextResult>): SuiteResult {
    val successfulContexts = resolvedContexts.filterIsInstance<ContextInfo>()
    val failedContexts: List<FailedContext> = resolvedContexts.filterIsInstance<FailedContext>()
    val results = successfulContexts.flatMap { it.tests.values }.awaitAll()
    successfulContexts.forEach {
        it.afterSuiteCallbacks.forEach { callback ->
            try {
                callback.invoke()
            } catch (ignored: Exception) {
            } catch (ignored: AssertionError) {
            }
        }
    }
    return SuiteResult(
        results,
        results.filter { it.isFailed },
        successfulContexts.flatMap { it.contexts },
        failedContexts

    )
}
internal fun printResults(coroutineScope: CoroutineScope, contextInfos: List<Deferred<ContextResult>>) {
    contextInfos.forEach {
        coroutineScope.launch {
            val context = it.await()
            val contextTreeReporter = ContextTreeReporter()
            when (context) {
                is ContextInfo -> {
                    println(
                        contextTreeReporter.stringReport(
                            context.tests.values.awaitAll(),
                            context.contexts
                        )
                            .joinToString("\n")
                    )
                }
                is FailedContext -> {
                    println("context ${context.context} failed: ${context.failure.stackTraceToString()}")
                }
            }
        }
    }
}

internal fun pluralize(count: Int, item: String) = if (count == 1) "1 $item" else "$count ${item}s"
fun Suite(rootContexts: Collection<RootContext>) =
    Suite(rootContexts.map { ContextProvider { listOf(it) } })

fun Suite(kClasses: List<KClass<*>>) =
    Suite(kClasses.map { ObjectContextProvider(it) })

fun Suite(rootContext: RootContext) = Suite(listOf(ContextProvider { listOf(rootContext) }))
fun Suite(lambda: ContextLambda) = Suite(RootContext("root", false, 0, function = lambda))
fun cpus() = Runtime.getRuntime().availableProcessors()
