package failgood.junit.next

import failgood.Context
import failgood.ExecutionListener
import failgood.FailGoodException
import failgood.Failure
import failgood.Skipped
import failgood.Success
import failgood.TestContainer
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.awaitTestResults
import failgood.internal.LoadResults
import failgood.internal.SuiteExecutionContext
import failgood.junit.ContextFinder
import failgood.junit.FailGoodJunitTestEngineConstants
import failgood.junit.TestMapper
import failgood.junit.exp.DynamicTestDescriptor
import failgood.junit.exp.TestPlanNode
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import java.util.concurrent.ConcurrentHashMap

class NewJunitEngine : TestEngine {
    override fun getId(): String = "failgood-new"

    override fun discover(
        discoveryRequest: EngineDiscoveryRequest,
        uniqueId: UniqueId
    ): TestDescriptor {
        val suiteExecutionContext = SuiteExecutionContext()

        val runTestFixtures =
            discoveryRequest.configurationParameters
                .getBoolean(FailGoodJunitTestEngineConstants.RUN_TEST_FIXTURES)
                .orElse(false)
        val suiteAndFilters = ContextFinder(runTestFixtures).findContexts(discoveryRequest)
        suiteAndFilters
            ?: return EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.displayName)
        val loadResults =
            runBlocking(suiteExecutionContext.coroutineDispatcher) {
                suiteAndFilters.suite.getRootContexts(suiteExecutionContext.scope)
            }

        return FailGoodEngineDescriptor(uniqueId, id, loadResults, suiteExecutionContext)
    }

    override fun execute(request: ExecutionRequest) {
        val startedContexts = ConcurrentHashMap.newKeySet<TestContainer>()

        val root = request.rootTestDescriptor
        if (root !is FailGoodEngineDescriptor) return
        request.engineExecutionListener.executionStarted(root)
        try {
            val testMapper = TestMapper()
            runBlocking(root.suiteExecutionContext.coroutineDispatcher) {
                val r = root.loadResults.investigate(
                    root.suiteExecutionContext.scope,
                    listener = NewExecutionListener(
                        root,
                        request.engineExecutionListener,
                        startedContexts,
                        testMapper
                    )
                ).awaitAll()
                awaitTestResults(r)
            }
            val leafToRootContexts = startedContexts.sortedBy { -it.parents.size }
            leafToRootContexts.forEach { context ->
                request.engineExecutionListener.executionFinished(
                    testMapper.getMapping(context),
                    TestExecutionResult.successful()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal class FailGoodEngineDescriptor(
        uniqueId: UniqueId?,
        displayName: String?,
        val loadResults: LoadResults,
        val suiteExecutionContext: SuiteExecutionContext
    ) : EngineDescriptor(uniqueId, displayName)
}

internal class NewExecutionListener(
    private val root: NewJunitEngine.FailGoodEngineDescriptor,
    private val listener: EngineExecutionListener,
    private val startedContexts: MutableSet<TestContainer>,
    private val testMapper: TestMapper
) :
    ExecutionListener {
        override suspend fun testDiscovered(testDescription: TestDescription) {
            val parent = testMapper.getMapping(testDescription.container)
            val node = TestPlanNode.Test(testDescription.testName)
            val descriptor = DynamicTestDescriptor(node, parent)
            testMapper.addMapping(testDescription, descriptor)

            listener.dynamicTestRegistered(descriptor)
        }

        override suspend fun contextDiscovered(context: Context) {
            val node = TestPlanNode.Container(context.name)
            val parent =
                if (context.parent == null) root else testMapper.getMapping(context.parent)
            val descriptor = DynamicTestDescriptor(node, parent)
            testMapper.addMapping(context, descriptor)
            listener.dynamicTestRegistered(descriptor)
        }

        override suspend fun testStarted(testDescription: TestDescription) {
            val descriptor = testMapper.getMappingOrNull(testDescription)
                ?: throw FailGoodException("mapping for $testDescription not found")
            startParentContexts(testDescription)
            listener.executionStarted(descriptor)
        }

        private fun startParentContexts(testDescription: TestDescription) {
            val context = testDescription.container
            (context.parents + context).forEach {
                if (startedContexts.add(it)) listener.executionStarted(testMapper.getMapping(it))
            }
        }

        override suspend fun testFinished(testPlusResult: TestPlusResult) {
            val descriptor = testMapper.getMappingOrNull(testPlusResult.test)
                ?: throw FailGoodException("mapping for $testPlusResult not found")
            when (testPlusResult.result) {
                is Failure -> listener.executionFinished(
                    descriptor,
                    TestExecutionResult.failed(testPlusResult.result.failure)
                )

                is Skipped -> listener.executionSkipped(descriptor, testPlusResult.result.reason)
                is Success -> listener.executionFinished(descriptor, TestExecutionResult.successful())
            }
        }

        override suspend fun testEvent(testDescription: TestDescription, type: String, payload: String) {
        }
    }
