package failgood.internal.execution

import failgood.*
import failgood.internal.*
import failgood.internal.given.RootGivenDSLHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.withTimeout

internal class TestCollectionExecutor<RootGiven>(
    private val testCollection: TestCollection<RootGiven>,
    scope: CoroutineScope,
    lazy: Boolean = false,
    listener: ExecutionListener = NullExecutionListener,
    testFilter: TestFilter = ExecuteAllTests,
    timeoutMillis: Long = 40000L,
    runOnlyTag: String? = null
) {
    companion object {
        init {
            // this class is used in a catch (Throwable) block so make sure we always have this
            // class available even when we are out of files when we first need it
            Class.forName(FailedTestCollectionExecution::class.qualifiedName)
        }
    }

    private val staticExecutionConfig =
        StaticContextExecutionConfig(
            testCollection.function,
            scope,
            listener,
            testFilter,
            timeoutMillis,
            if (lazy) CoroutineStart.LAZY else CoroutineStart.DEFAULT,
            runOnlyTag,
            testCollection.given)

    private val stateCollector =
        ContextStateCollector(staticExecutionConfig, !testCollection.rootContext.isolation)

    /**
     * Execute the rootContext.
     *
     * We keep executing the Context DSL until we know about all contexts and tests in this root
     * context The first test in each context is directly executed (async via coroutines), and for
     * all other tests in that context we create a SingleTestExecutor that executes the whole
     * context path of that test together with the test.
     */
    suspend fun execute(): TestCollectionExecutionResult {
        if (!staticExecutionConfig.testFilter.shouldRun(testCollection))
            return TestResults(listOf(), mapOf(), setOf())
        val function = testCollection.function
        val rootContext = testCollection.rootContext
        staticExecutionConfig.listener.contextDiscovered(rootContext)
        try {
            do {
                val startTime = System.nanoTime()
                val resourcesCloser = ResourceCloserImpl(staticExecutionConfig.scope)
                val visitor =
                    ContextVisitor(
                        staticExecutionConfig,
                        stateCollector,
                        rootContext,
                        resourcesCloser,
                        false,
                        stateCollector.investigatedContexts.contains(rootContext),
                        startTime,
                        RootGivenDSLHandler(staticExecutionConfig.givenFunction))
                try {
                    withTimeout(staticExecutionConfig.timeoutMillis) { visitor.function() }
                } catch (_: ContextFinished) {}
                stateCollector.investigatedContexts.add(rootContext)
                if (stateCollector.containsContextsWithoutIsolation) {
                    stateCollector.afterSuiteCallbacks.add { resourcesCloser.closeAutoCloseables() }
                }
            } while (visitor.contextsLeft)
        } catch (e: Throwable) {
            return FailedTestCollectionExecution(rootContext, e)
        }
        // context order: first root context, then sub-contexts ordered by line number
        val contexts =
            listOf(rootContext) +
                stateCollector.foundContexts.sortedBy { it.sourceInfo!!.lineNumber }
        return TestResults(
            contexts, stateCollector.deferredTestResults, stateCollector.afterSuiteCallbacks)
    }
}

// this is thrown to save time when the context is finished, and we cannot do anything meaningful in
// this pass
class ContextFinished : DSLGotoException()

fun sourceInfo(): SourceInfo {
    // find the first stack trace element not in this class or ContextDSL
    // (ContextDSL because of default parameters defined there)
    val first =
        RuntimeException().stackTrace.first {
            !(it.fileName?.let { fileName ->
                fileName.endsWith("ContextVisitor.kt") ||
                    fileName.endsWith("TestCollectionExecutor.kt") ||
                    fileName.endsWith("ContextDSL.kt")
            } ?: true)
        }
    return first.let { SourceInfo(it.className, it.fileName!!, it.lineNumber) }
}

internal class DuplicateNameInContextException(s: String) : FailGoodException(s)

internal class ImmutableContextException(s: String) : FailGoodException(s)
