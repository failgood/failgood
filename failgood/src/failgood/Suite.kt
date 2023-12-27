package failgood

import failgood.dsl.ContextFunction
import failgood.internal.ContextInfo
import failgood.internal.ContextResult
import failgood.internal.ContextTreeReporter
import failgood.internal.ExecuteAllTestFilterProvider
import failgood.internal.FailedRootContext
import failgood.internal.LoadResults
import failgood.internal.SuiteExecutionContext
import failgood.internal.TestFilterProvider
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

const val DEFAULT_TIMEOUT: Long = 40000

data class Suite(val contextProviders: Collection<ContextProvider>) {
    init {
        if (contextProviders.isEmpty()) throw EmptySuiteException()
    }

    fun run(
        parallelism: Int? = null,
        silent: Boolean = false,
        filter: TestFilterProvider? = null,
        listener: ExecutionListener = NullExecutionListener
    ): SuiteResult {
        return SuiteExecutionContext(parallelism).use { suiteExecutionContext ->
            suiteExecutionContext.coroutineDispatcher.use { dispatcher ->
                runBlocking(dispatcher) {
                    val contextInfos =
                        findTests(
                            this,
                            filter = filter ?: ExecuteAllTestFilterProvider,
                            listener = listener
                        )
                    if (!silent) {
                        printResults(this, contextInfos)
                    }
                    awaitTestResults(contextInfos.awaitAll())
                }
            }
        }
    }

    companion object {
        internal fun parseTimeout(timeout: String?): Long {
            return when (timeout) {
                null -> DEFAULT_TIMEOUT
                "" -> Long.MAX_VALUE
                else ->
                    timeout.toLongOrNull()
                        ?: throw FailGoodException("TIMEOUT must be a number or an empty string")
            }
        }
    }

    internal suspend fun findTests(
        coroutineScope: CoroutineScope,
        executeTests: Boolean = true,
        filter: TestFilterProvider = ExecuteAllTestFilterProvider,
        listener: ExecutionListener = NullExecutionListener
    ): List<Deferred<ContextResult>> {
        return getRootContexts(coroutineScope)
            .investigate(coroutineScope, executeTests, filter, listener)
    }

    private suspend fun getRootContexts(coroutineScope: CoroutineScope): LoadResults =
        LoadResults(
            contextProviders
                .map {
                    coroutineScope.async {
                        try {
                            it.getContexts()
                        } catch (e: ErrorLoadingContextsFromClass) {
                            listOf(CouldNotLoadTestCollection(e, e.kClass))
                        }
                    }
                }
                .flatMap { it.await() }
                .sortedBy { it.order }
        )
}

internal object NullExecutionListener : ExecutionListener {
    override suspend fun testStarted(testDescription: TestDescription) {}

    override suspend fun testFinished(testPlusResult: TestPlusResult) {}

    override suspend fun testEvent(
        testDescription: TestDescription,
        type: String,
        payload: String
    ) {}
}

internal suspend fun awaitTestResults(resolvedContexts: List<ContextResult>): SuiteResult {
    val successfulContexts = resolvedContexts.filterIsInstance<ContextInfo>()
    val failedRootContexts: List<FailedRootContext> =
        resolvedContexts.filterIsInstance<FailedRootContext>()
    val results = successfulContexts.flatMap { it.tests.values }.awaitAll()
    successfulContexts.forEach {
        it.afterSuiteCallbacks.forEach { callback ->
            try {
                callback.invoke()
                // here we don't catch throwable because we are already finished anyway.
            } catch (ignored: Exception) {} catch (ignored: AssertionError) {}
        }
    }
    return SuiteResult(
        results,
        results.filter { it.isFailure },
        successfulContexts.flatMap { it.contexts },
        failedRootContexts
    )
}

internal fun printResults(
    coroutineScope: CoroutineScope,
    contextInfos: List<Deferred<ContextResult>>
) {
    contextInfos.forEach {
        coroutineScope.launch {
            val context = it.await()
            val contextTreeReporter = ContextTreeReporter()
            when (context) {
                is ContextInfo -> {
                    println(
                        contextTreeReporter
                            .stringReport(context.tests.values.awaitAll(), context.contexts)
                            .joinToString("\n")
                    )
                }
                is FailedRootContext -> {
                    println(
                        "context ${context.context} failed: ${context.failure.stackTraceToString()}"
                    )
                }
            }
        }
    }
}

fun Suite(rootContexts: Collection<TestCollection<*>>) =
    Suite(rootContexts.map { ContextProvider { listOf(it) } })

fun Suite(kClasses: List<KClass<*>>) = Suite(kClasses.map { ObjectContextProvider(it) })

fun <RootGiven> Suite(rootContext: TestCollection<RootGiven>) = Suite(listOf(rootContext))

fun Suite(function: ContextFunction) = Suite(TestCollection("root", order = 0, function = function))
