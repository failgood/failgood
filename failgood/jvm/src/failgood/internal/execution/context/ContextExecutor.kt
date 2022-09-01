package failgood.internal.execution.context

import failgood.*
import failgood.internal.*
import kotlinx.coroutines.*

internal class ContextExecutor constructor(
    val rootContext: RootContext,
    val scope: CoroutineScope,
    lazy: Boolean = false,
    val listener: ExecutionListener = NullExecutionListener,
    val testFilter: TestFilter = ExecuteAllTests,
    val timeoutMillis: Long = 40000L,
    val onlyTag: String? = null
) {
    val filteringByTag = onlyTag != null
    val coroutineStart: CoroutineStart = if (lazy) CoroutineStart.LAZY else CoroutineStart.DEFAULT
    var startTime = System.nanoTime()

    // did we find contexts without isolation in this root context?
    // in that case we have to call the resources closer after suite.
    var containsContextsWithoutIsolation = !rootContext.isolation

    val foundContexts = mutableListOf<Context>()
    val deferredTestResults = LinkedHashMap<TestDescription, Deferred<TestPlusResult>>()
    val processedTests = LinkedHashSet<ContextPath>()
    val afterSuiteCallbacks = mutableSetOf<suspend () -> Unit>()
    val investigatedContexts = mutableSetOf<Context>()

    /**
     * Execute the rootContext.
     *
     * We keep executing the Context DSL until we know about all contexts and tests in this root context
     * The first test in each context is directly executed (async via coroutines), and for all other tests in that
     * context we create a SingleTestExecutor that executes the whole context path of that test together with the test.
     *
     */
    suspend fun execute(): ContextResult {
        if (!testFilter.shouldRun(rootContext))
            return ContextInfo(listOf(), mapOf(), setOf())
        val function = rootContext.function
        val rootContext = Context(rootContext.name, null, rootContext.sourceInfo, rootContext.isolation)
        try {
            withTimeout(timeoutMillis) {
                do {
                    startTime = System.nanoTime()
                    val resourcesCloser = ResourcesCloser(scope)
                    val visitor = ContextVisitor(
                        this@ContextExecutor,
                        rootContext,
                        resourcesCloser,
                        given = {}

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

    suspend fun recordContextAsFailed(
        context: Context,
        sourceInfo: SourceInfo,
        contextPath: ContextPath,
        exceptionInContext: Throwable
    ) {
        val testDescriptor = TestDescription(context, "error in context", sourceInfo)

        processedTests.add(contextPath) // don't visit this context again
        val testPlusResult = TestPlusResult(testDescriptor, Failure(exceptionInContext))
        deferredTestResults[testDescriptor] = CompletableDeferred(testPlusResult)
        foundContexts.add(context)
        listener.testFinished(testPlusResult)
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
