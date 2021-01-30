package failfast.internal

import failfast.Context
import failfast.TestDescription
import failfast.TestResult
import kotlinx.coroutines.Deferred

internal data class ContextInfo(
    val contexts: List<Context>,
    val tests: Map<TestDescription, Deferred<TestResult>>
)
