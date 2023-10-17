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
import failgood.junit.DEBUG_TXT_FILENAME
import failgood.junit.FailGoodJunitTestEngineConstants
import failgood.junit.FailureLogger
import failgood.junit.FailureLoggingEngineExecutionListener
import failgood.junit.LoggingEngineExecutionListener
import failgood.junit.TestMapper
import failgood.junit.niceString
import java.io.File
import java.util.concurrent.ConcurrentHashMap
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

class NewJunitEngine : TestEngine {
    override fun getId(): String = "failgood-new"

    private var debug: Boolean = false
    private val failureLogger = FailureLogger()

    override fun discover(
        discoveryRequest: EngineDiscoveryRequest,
        uniqueId: UniqueId
    ): TestDescriptor {
        debug =
            discoveryRequest.configurationParameters
                .getBoolean(FailGoodJunitTestEngineConstants.CONFIG_KEY_DEBUG)
                .orElse(false)

        failureLogger.add("discoveryRequest", discoveryRequest.niceString())
        val suiteExecutionContext = SuiteExecutionContext()

        val runTestFixtures =
            discoveryRequest.configurationParameters
                .getBoolean(FailGoodJunitTestEngineConstants.RUN_TEST_FIXTURES)
                .orElse(false)
        val suiteAndFilters = ContextFinder(runTestFixtures).findContexts(discoveryRequest)
        suiteAndFilters
            ?: return EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.DISPLAY_NAME)
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
        val loggingEngineExecutionListener =
            LoggingEngineExecutionListener(request.engineExecutionListener)
        val listener =
            FailureLoggingEngineExecutionListener(loggingEngineExecutionListener, failureLogger)
        listener.executionStarted(root)
        try {
            val testMapper = TestMapper()
            val results =
                runBlocking(root.suiteExecutionContext.coroutineDispatcher) {
                    val r =
                        root.loadResults
                            .investigate(
                                root.suiteExecutionContext.scope,
                                listener =
                                    NewExecutionListener(
                                        root,
                                        listener,
                                        startedContexts,
                                        testMapper
                                    )
                            )
                            .awaitAll()
                    awaitTestResults(r)
                }
            // report the failing root contexts
            results.failedRootContexts.forEach {
                val node = TestPlanNode.Container(it.context.name)
                val testDescriptor = DynamicTestDescriptor(node, root)

                listener.dynamicTestRegistered(testDescriptor)
                listener.executionStarted(testDescriptor)
                listener.executionFinished(testDescriptor, TestExecutionResult.failed(it.failure))
            }
            // close all open contexts.
            val leafToRootContexts = startedContexts.sortedBy { -it.parents.size }
            leafToRootContexts.forEach { context ->
                listener.executionFinished(
                    testMapper.getMapping(context),
                    TestExecutionResult.successful()
                )
            }
            listener.executionFinished(root, TestExecutionResult.successful())
            failureLogger
        } catch (e: Exception) {
            failureLogger.add("events", loggingEngineExecutionListener.events.toString())
            failureLogger.fail(e)
        } finally {
            if (debug) {
                failureLogger.add("events", loggingEngineExecutionListener.events.toString())
                File(DEBUG_TXT_FILENAME).writeText(failureLogger.envString())
            }
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
) : ExecutionListener {
    override suspend fun testDiscovered(testDescription: TestDescription) {
        val parent = testMapper.getMapping(testDescription.container)
        val node = TestPlanNode.Test(testDescription.testName)
        val descriptor = DynamicTestDescriptor(node, parent)
        testMapper.addMapping(testDescription, descriptor)

        listener.dynamicTestRegistered(descriptor)
    }

    override suspend fun contextDiscovered(context: Context) {
        val node = TestPlanNode.Container(context.name)
        val descriptor =
            if (context.parent == null) {
                DynamicTestDescriptor(
                    node,
                    root,
                    "${context.name}(${(context.sourceInfo?.className) ?: ""})"
                )
            } else {
                DynamicTestDescriptor(node, testMapper.getMapping(context.parent))
            }
        testMapper.addMapping(context, descriptor)
        listener.dynamicTestRegistered(descriptor)
    }

    override suspend fun testStarted(testDescription: TestDescription) {
        val descriptor =
            testMapper.getMappingOrNull(testDescription)
                ?: throw FailGoodException("mapping for $testDescription not found")
        startParentContexts(testDescription)
        listener.executionStarted(descriptor)
    }

    private fun startParentContexts(testDescription: TestDescription) {
        val context = testDescription.container
        (context.parents + context).forEach {
            if (startedContexts.add(it)) {
                listener.executionStarted(testMapper.getMapping(it))
            }
        }
    }

    override suspend fun testFinished(testPlusResult: TestPlusResult) {
        val testDescription = testPlusResult.test
        val descriptor =
            testMapper.getMappingOrNull(testDescription)
                ?: throw FailGoodException("mapping for $testDescription not found")
        when (testPlusResult.result) {
            is Failure ->
                listener.executionFinished(
                    descriptor,
                    TestExecutionResult.failed(testPlusResult.result.failure)
                )
            is Skipped -> {
                // for skipped tests testStarted is not called, so we have to start parent contexts
                // here.
                startParentContexts(testDescription)
                listener.executionSkipped(descriptor, testPlusResult.result.reason)
            }
            is Success -> listener.executionFinished(descriptor, TestExecutionResult.successful())
        }
    }

    override suspend fun testEvent(
        testDescription: TestDescription,
        type: String,
        payload: String
    ) {}
}
