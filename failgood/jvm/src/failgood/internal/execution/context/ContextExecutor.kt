package failgood.internal.execution.context

import failgood.*
import failgood.internal.*
import kotlinx.coroutines.*

internal class ContextExecutor(
    private val rootContext: RootContext,
    scope: CoroutineScope,
    lazy: Boolean = false,
    listener: ExecutionListener = NullExecutionListener,
    testFilter: TestFilter = ExecuteAllTests,
    timeoutMillis: Long = 40000L,
    runOnlyTag: String? = null
) : ContextStateCollector {
    private val staticExecutionConfig = StaticContextExecutionConfig(
        rootContext,
        scope,
        listener,
        testFilter,
        timeoutMillis,
        if (lazy) CoroutineStart.LAZY else CoroutineStart.DEFAULT,
        runOnlyTag
    )
    // did we find contexts without isolation in this root context?
    // in that case we have to call the resources closer after suite.
    override var containsContextsWithoutIsolation = !rootContext.isolation

    // here we build a list of all the sub-contexts in this root context to later return it
    override val foundContexts = mutableListOf<Context>()

    override val deferredTestResults = LinkedHashMap<TestDescription, Deferred<TestPlusResult>>()
    override val afterSuiteCallbacks = mutableSetOf<suspend () -> Unit>()

    // a context is investigated when we have executed it once. we still need to execute it again to get into its sub-contexts
    override val investigatedContexts = mutableSetOf<Context>()

    // tests or contexts that we don't have to execute again.
    override val finishedPaths = LinkedHashSet<ContextPath>()

    /**
     * Execute the rootContext.
     *
     * We keep executing the Context DSL until we know about all contexts and tests in this root context
     * The first test in each context is directly executed (async via coroutines), and for all other tests in that
     * context we create a SingleTestExecutor that executes the whole context path of that test together with the test.
     *
     */
    suspend fun execute(): ContextResult {
        if (!staticExecutionConfig.testFilter.shouldRun(rootContext))
            return ContextInfo(listOf(), mapOf(), setOf())
        val function = rootContext.function
        val rootContext = Context(rootContext.name, null, rootContext.sourceInfo, rootContext.isolation)
        try {
            withTimeout(staticExecutionConfig.timeoutMillis) {
                do {
                    val startTime = System.nanoTime()
                    val resourcesCloser = OnlyResourcesCloser(staticExecutionConfig.scope)
                    val visitor = ContextVisitor(
                        staticExecutionConfig,
                        this@ContextExecutor,
                        rootContext,
                        {},
                        resourcesCloser,
                        false,
                        investigatedContexts.contains(rootContext),
                        startTime
                    )
                    visitor.function()
                    investigatedContexts.add(rootContext)
                    if (containsContextsWithoutIsolation) {
                        afterSuiteCallbacks.add { resourcesCloser.closeAutoCloseables() }
                    }
                } while (visitor.contextsLeft)
            }
        } catch (e: Throwable) {
            return FailedRootContext(rootContext, e)
        }
        // context order: first root context, then sub-contexts ordered by line number
        val contexts = listOf(rootContext) + foundContexts.sortedBy { it.sourceInfo!!.lineNumber }
        return ContextInfo(contexts, deferredTestResults, afterSuiteCallbacks)
    }

    override suspend fun recordContextAsFailed(
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
        staticExecutionConfig.listener.testFinished(testPlusResult)
    }
}

fun sourceInfo(): SourceInfo {
    val runtimeException = RuntimeException()
    // find the first stack trace element that is not in this class or ContextDSL
    // (ContextDSL because of default parameters defined there)
    return SourceInfo(
        runtimeException.stackTrace.first {
            !(
                it.fileName?.let { fileName ->
                    fileName.endsWith("ContextVisitor.kt") ||
                        fileName.endsWith("ContextExecutor.kt") ||
                        fileName.endsWith("ContextDSL.kt")
                } ?: true
                )
        }!!
    )
}

internal class DuplicateNameInContextException(s: String) : FailGoodException(s)
internal class ImmutableContextException(s: String) : FailGoodException(s)
