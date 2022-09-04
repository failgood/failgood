package failgood.internal.execution.context

import failgood.*
import failgood.internal.ContextPath
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

internal class ContextStateCollector(
    val listener: ExecutionListener,
    // did we find contexts without isolation in this root context?
    // in that case we have to call the resources closer after suite.
    var containsContextsWithoutIsolation: Boolean
) {

    // here we build a list of all the sub-contexts in this root context to later return it
    val foundContexts = mutableListOf<Context>()

    val deferredTestResults = LinkedHashMap<TestDescription, Deferred<TestPlusResult>>()
    val afterSuiteCallbacks = mutableSetOf<suspend () -> Unit>()

    // a context is investigated when we have executed it once. we still need to execute it again to get into its sub-contexts
    val investigatedContexts = mutableSetOf<Context>()

    // tests or contexts that we don't have to execute again.
    val finishedPaths = LinkedHashSet<ContextPath>()

    suspend fun recordContextAsFailed(
        context: Context,
        sourceInfo: SourceInfo,
        contextPath: ContextPath,
        exceptionInContext: Throwable
    ) {
        val testDescriptor = TestDescription(context, "error in context", sourceInfo)

        finishedPaths.add(contextPath) // don't visit this context again
        val testPlusResult = TestPlusResult(testDescriptor, Failure(exceptionInContext))
        deferredTestResults[testDescriptor] = CompletableDeferred(testPlusResult)
        foundContexts.add(context)
        listener.testFinished(testPlusResult)
    }
}
