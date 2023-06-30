package failgood

import failgood.internal.*
import failgood.internal.ContextInfo
import failgood.internal.ContextTreeReporter
import failgood.internal.ExecuteAllTestFilterProvider
import failgood.internal.TestFilterProvider
import failgood.internal.execution.context.ContextExecutor
import failgood.internal.util.getenv
import failgood.internal.util.pluralize
import kotlinx.coroutines.*
import java.lang.management.ManagementFactory
import kotlin.reflect.KClass

const val DEFAULT_TIMEOUT: Long = 40000

class Suite(val contextProviders: Collection<ContextProvider>) {
    init {
        if (contextProviders.isEmpty()) throw EmptySuiteException()
    }

    fun run(parallelism: Int = cpus(), silent: Boolean = false): SuiteResult {
        return SuiteExecutionContext(parallelism).use { suiteExecutionContext ->
            suiteExecutionContext.coroutineDispatcher
                .use { dispatcher ->
                    runBlocking(dispatcher) {
                        val contextInfos = findTests(this)
                        if (!silent) {
                            printResults(this, contextInfos)
                        }
                        awaitTestResults(contextInfos.awaitAll())
                    }
                }
        }
    }

    // set timeout to the timeout in milliseconds, an empty string to turn it off
    private val timeoutMillis: Long = parseTimeout(getenv("TIMEOUT"))

    companion object {
        internal fun parseTimeout(timeout: String?): Long {
            return when (timeout) {
                null -> DEFAULT_TIMEOUT
                "" -> Long.MAX_VALUE
                else -> timeout.toLongOrNull() ?: throw FailGoodException("TIMEOUT must be a number or an empty string")
            }
        }
    }

    internal suspend fun findTests(
        coroutineScope: CoroutineScope,
        executeTests: Boolean = true,
        executionFilter: TestFilterProvider = ExecuteAllTestFilterProvider,
        listener: ExecutionListener = NullExecutionListener
    ): List<Deferred<ContextResult>> {
        val tag = getenv("FAILGOOD_TAG")
        return getRootContexts(coroutineScope)
            .map { context: LoadResult ->
                when (context) {
                    is CouldNotLoadContext ->
                        CompletableDeferred(
                            FailedRootContext(Context(context.jClass.name ?: "unknown"), context.reason)
                        )
                    is RootContext -> {
                        val testFilter = executionFilter.forClass(context.sourceInfo.className)
                        coroutineScope.async {
                            if (context.ignored?.isIgnored() == null) {
                                ContextExecutor(
                                    context,
                                    coroutineScope,
                                    !executeTests,
                                    listener,
                                    testFilter,
                                    timeoutMillis,
                                    runOnlyTag = tag
                                ).execute()
                            } else
                                ContextInfo(emptyList(), mapOf(), setOf())
                        }
                    }
                }
            }
    }

    internal suspend fun getRootContexts(coroutineScope: CoroutineScope): List<LoadResult> = contextProviders
        .map {
            coroutineScope.async {
                try {
                    it.getContexts()
                } catch (e: ErrorLoadingContextsFromClass) {
                    listOf(CouldNotLoadContext(e, e.jClass))
                }
            }
        }.flatMap { it.await() }.sortedBy { it.order }
}

internal object NullExecutionListener : ExecutionListener {
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
    return "${uptime}ms. load:$percentage%." + if (totalTests != null) {
        " " + pluralize(totalTests * 1000 / uptime.toInt(), "test") + "/sec"
    } else
        ""
}

internal suspend fun awaitTestResults(resolvedContexts: List<ContextResult>): SuiteResult {
    val successfulContexts = resolvedContexts.filterIsInstance<ContextInfo>()
    val failedRootContexts: List<FailedRootContext> = resolvedContexts.filterIsInstance<FailedRootContext>()
    val results = successfulContexts.flatMap { it.tests.values }.awaitAll()
    successfulContexts.forEach {
        it.afterSuiteCallbacks.forEach { callback ->
            try {
                callback.invoke()
                // here we don't catch throwable because we are already finished anyway.
            } catch (ignored: Exception) {
            } catch (ignored: AssertionError) {
            }
        }
    }
    return SuiteResult(
        results,
        results.filter { it.isFailure },
        successfulContexts.flatMap { it.contexts },
        failedRootContexts

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
                is FailedRootContext -> {
                    println("context ${context.context} failed: ${context.failure.stackTraceToString()}")
                }
            }
        }
    }
}

fun Suite(rootContexts: Collection<RootContext>) =
    Suite(rootContexts.map { ContextProvider { listOf(it) } })

fun Suite(kClasses: List<KClass<*>>) =
    Suite(kClasses.map { ObjectContextProvider(it) })

fun Suite(rootContext: RootContext) = Suite(listOf(ContextProvider { listOf(rootContext) }))
fun Suite(lambda: ContextLambda) = Suite(RootContext("root", order = 0, function = lambda))
fun cpus() = Runtime.getRuntime().availableProcessors()
