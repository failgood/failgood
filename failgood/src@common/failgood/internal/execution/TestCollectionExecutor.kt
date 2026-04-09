package failgood.internal.execution

import failgood.ExecutionListener
import failgood.FailGoodException
import failgood.NullExecutionListener
import failgood.TestCollection
import failgood.internal.DSLGotoException
import failgood.internal.ExecuteAllTests
import failgood.internal.FailedTestCollectionExecution
import failgood.internal.TestCollectionExecutionResult
import failgood.internal.TestFilter
import failgood.internal.TestResults
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
    runOnlyTag: String? = null,
) {
    companion object {
        init {
            preloadFailedTestCollectionExecution()
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
            testCollection.given,
        )

    private val stateCollector =
        ContextStateCollector(staticExecutionConfig, !testCollection.rootContext.isolation)

    suspend fun execute(): TestCollectionExecutionResult {
        if (!staticExecutionConfig.testFilter.shouldRun(testCollection)) {
            return TestResults(listOf(), mapOf(), setOf())
        }
        val function = testCollection.function
        val rootContext = testCollection.rootContext
        staticExecutionConfig.listener.contextDiscovered(rootContext)
        try {
            do {
                val startTime = platformNanoTime()
                val resourcesCloser = createResourcesCloser(staticExecutionConfig.scope)
                val visitor =
                    ContextVisitor(
                        staticExecutionConfig,
                        stateCollector,
                        rootContext,
                        resourcesCloser,
                        false,
                        stateCollector.investigatedContexts.contains(rootContext),
                        startTime,
                        RootGivenDSLHandler(staticExecutionConfig.givenFunction),
                    )
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
        val contexts =
            listOf(rootContext) +
                stateCollector.foundContexts.sortedBy { it.sourceInfo!!.lineNumber }
        return TestResults(
            contexts,
            stateCollector.deferredTestResults,
            stateCollector.afterSuiteCallbacks,
        )
    }
}

class ContextFinished : DSLGotoException()

internal class DuplicateNameInContextException(s: String) : FailGoodException(s)

internal class ImmutableContextException(s: String) : FailGoodException(s)
