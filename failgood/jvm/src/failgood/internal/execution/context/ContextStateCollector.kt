package failgood.internal.execution.context

import failgood.*
import failgood.internal.ContextPath
import failgood.internal.ContextResult
import failgood.internal.TestFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred

internal interface ContextStateCollector {
    val rootContext: RootContext
    val scope: CoroutineScope
    val listener: ExecutionListener
    val testFilter: TestFilter
    val timeoutMillis: Long
    val runOnlyTag: String?
    val coroutineStart: CoroutineStart
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

    /**
     * Execute the rootContext.
     *
     * We keep executing the Context DSL until we know about all contexts and tests in this root context
     * The first test in each context is directly executed (async via coroutines), and for all other tests in that
     * context we create a SingleTestExecutor that executes the whole context path of that test together with the test.
     *
     */
    suspend fun execute(): ContextResult
    suspend fun recordContextAsFailed(
        context: Context,
        sourceInfo: SourceInfo,
        contextPath: ContextPath,
        exceptionInContext: Throwable
    )
}
