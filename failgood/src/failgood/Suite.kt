package failgood

import failgood.dsl.ContextFunction
import failgood.internal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import kotlin.reflect.KClass

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
        return SuiteExecutionContext(parallelism).use { suiteExecutionContext ->
            if (!silent)
                println("starting test suite with parallelism = ${suiteExecutionContext.parallelism}")
            suiteExecutionContext.coroutineDispatcher.use { dispatcher ->
                runBlocking(dispatcher + MDCContext()) {
                    val contextInfos =
                        findAndStartTests(
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
                .sortedBy { it.order }
        )
    }
}

internal object NullExecutionListener : ExecutionListener {
    override suspend fun testStarted(testDescription: TestDescription) {}

    override suspend fun testFinished(testPlusResult: TestPlusResult) {}

    override suspend fun testEvent(
        testDescription: TestDescription,
        type: String,
        payload: String
    ) {
    }
}

internal suspend fun awaitTestResults(resolvedContexts: List<TestCollectionExecutionResult>): SuiteResult {
    val successfulContexts = resolvedContexts.filterIsInstance<TestResults>()
    val failedRootContexts: List<FailedTestCollectionExecution> =
        resolvedContexts.filterIsInstance<FailedTestCollectionExecution>()
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

internal fun printResults(
    coroutineScope: CoroutineScope,
    contextInfos: List<Deferred<TestCollectionExecutionResult>>
) {
    contextInfos.forEach {
        coroutineScope.launch {
            val context = it.await()
            val contextTreeReporter = ContextTreeReporter()
            when (context) {
                is TestResults -> {
                    println(
                        contextTreeReporter
                            .stringReport(context.tests.values.awaitAll(), context.contexts)
                            .joinToString("\n")
                    )
                }

                is FailedTestCollectionExecution -> {
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

operator fun <T> List<T>.times(n: Int): List<T> = if (n == 1) this else
    List(n) { this }.flatten()
