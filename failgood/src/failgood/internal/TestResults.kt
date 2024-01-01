package failgood.internal

import failgood.Context
import failgood.TestDescription
import failgood.TestPlusResult
import kotlinx.coroutines.Deferred

sealed interface TestCollectionExecutionResult

internal data class TestResults(
    val contexts: List<Context>,
    val tests: Map<TestDescription, Deferred<TestPlusResult>>,
    val afterSuiteCallbacks: Set<suspend () -> Unit>
) : TestCollectionExecutionResult

data class FailedTestCollectionExecution(val context: Context, val failure: Throwable) : TestCollectionExecutionResult
