package failfast.internal

import failfast.Context
import failfast.TestDescriptor
import failfast.TestResult
import kotlinx.coroutines.Deferred

internal data class ContextInfo(
    val contexts: List<Context>,
    val tests: Map<TestDescriptor, Deferred<TestResult>>
)
