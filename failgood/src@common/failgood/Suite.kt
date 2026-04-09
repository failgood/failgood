package failgood

import failgood.dsl.ContextFunction
import failgood.internal.ExecuteAllTestFilterProvider
import failgood.internal.FailedTestCollectionExecution
import failgood.internal.LoadResults
import failgood.internal.TestCollectionExecutionResult
import failgood.internal.TestFilterProvider
import failgood.internal.TestResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

internal const val DEFAULT_TIMEOUT: Long = 40000

data class Suite(val contextProviders: Collection<ContextProvider>, val repeat: Int = 1) {
    init {
        if (contextProviders.isEmpty()) throw EmptySuiteException()
    }

    fun run(
        parallelism: Int? = 2,
        silent: Boolean = false,
        filter: TestFilterProvider? = null,
        listener: ExecutionListener = NullExecutionListener
    ): SuiteResult {
        return runSuiteBlocking(
            suite = this,
            parallelism = parallelism,
            silent = silent,
            filter = filter ?: ExecuteAllTestFilterProvider,
            listener = listener,
        )
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

    internal suspend fun findAndStartTests(
        coroutineScope: CoroutineScope,
        executeTests: Boolean = true,
        filter: TestFilterProvider = ExecuteAllTestFilterProvider,
        listener: ExecutionListener = NullExecutionListener
    ): List<Deferred<TestCollectionExecutionResult>> {
        return getRootContexts(coroutineScope)
            .investigate(coroutineScope, executeTests, filter, listener)
    }

    private suspend fun getRootContexts(coroutineScope: CoroutineScope): LoadResults {
        return LoadResults(
            (contextProviders
                    .map {
                        coroutineScope.async {
                            try {
                                it.getContexts()
                            } catch (e: ErrorLoadingContextsFromClass) {
                                listOf(CouldNotLoadTestCollection(e, e.kClass))
                            }
                        }
                    }
                    .flatMap { it.await() } * repeat)
                .sortedBy { it.order })
    }
}

internal expect fun runSuiteBlocking(
    suite: Suite,
    parallelism: Int?,
    silent: Boolean,
    filter: TestFilterProvider,
    listener: ExecutionListener
): SuiteResult

internal suspend fun awaitTestResults(
    resolvedContexts: List<TestCollectionExecutionResult>
): SuiteResult {
    val successfulContexts = resolvedContexts.filterIsInstance<TestResults>()
    val failedRootContexts: List<FailedTestCollectionExecution> =
        resolvedContexts.filterIsInstance<FailedTestCollectionExecution>()
    val results = successfulContexts.flatMap { it.tests.values }.awaitAll()
    successfulContexts.forEach {
        it.afterSuiteCallbacks.forEach { callback ->
            try {
                callback.invoke()
            } catch (ignored: Exception) {} catch (ignored: AssertionError) {}
        }
    }
    return SuiteResult(
        allTests = results,
        failedTests = results.filter { it.isFailure },
        contexts = successfulContexts.flatMap { it.contexts },
        failedRootContexts = failedRootContexts,
    )
}

fun Suite(rootContexts: Collection<TestCollection<*>>): Suite =
    Suite(rootContexts.map { ContextProvider { listOf(it) } })

fun <RootGiven> Suite(rootContext: TestCollection<RootGiven>): Suite = Suite(listOf(rootContext))

fun Suite(function: ContextFunction): Suite =
    Suite(TestCollection("root", order = 0, function = function))

operator fun <T> List<T>.times(n: Int): List<T> = if (n == 1) this else List(n) { this }.flatten()
