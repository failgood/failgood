package failgood.internal.execution.context

import failgood.Context
import failgood.SourceInfo
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.internal.ContextPath
import kotlinx.coroutines.Deferred

internal interface ContextStateCollector {
    var startTime: Long

    // did we find contexts without isolation in this root context?
    // in that case we have to call the resources closer after suite.
    var containsContextsWithoutIsolation: Boolean

    // here we build a list of all the sub-contexts in this root context to later return it
    val foundContexts: MutableList<Context>
    val deferredTestResults: LinkedHashMap<TestDescription, Deferred<TestPlusResult>>
    val afterSuiteCallbacks: MutableSet<suspend () -> Unit>

    // a context is investigated when we have executed it once. we still need to execute it again to get into its sub-contexts
    val investigatedContexts: MutableSet<Context>

    // tests or contexts that we don't have to execute again.
    val finishedPaths: LinkedHashSet<ContextPath>

    suspend fun recordContextAsFailed(
        context: Context,
        sourceInfo: SourceInfo,
        contextPath: ContextPath,
        exceptionInContext: Throwable
    )
}
