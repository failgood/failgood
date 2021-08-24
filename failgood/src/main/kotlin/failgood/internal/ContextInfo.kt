package failgood.internal

import failgood.Context
import failgood.TestDescription
import failgood.TestPlusResult
import kotlinx.coroutines.Deferred

sealed interface ContextResult
internal data class ContextInfo(
    val contexts: List<Context>,
    val tests: Map<TestDescription, Deferred<TestPlusResult>>,
    val afterSuiteCallbacks: Set<suspend () -> Unit>
) : ContextResult

internal data class FailedContext(val context: Context, val failure: Throwable) : ContextResult
