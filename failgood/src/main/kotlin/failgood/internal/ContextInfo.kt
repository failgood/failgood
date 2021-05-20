package failgood.internal

import failgood.Context
import failgood.TestDescription
import failgood.TestPlusResult
import kotlinx.coroutines.Deferred

internal data class ContextInfo(
    val contexts: List<Context>,
    val tests: Map<TestDescription, Deferred<TestPlusResult>>
)
