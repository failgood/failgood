package failgood.junit.next

import failgood.Context
import failgood.junit.ContextFinder
import failgood.junit.FailGoodJunitTestEngineConstants
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_SILENT
import failgood.junit.FailGoodJunitTestEngineConstants.DEBUG_TXT_FILENAME
import failgood.junit.FailureLogger
import failgood.junit.FailureLoggingEngineExecutionListener
import failgood.junit.LoggingEngineExecutionListener
import failgood.junit.SuiteAndFilters
import failgood.junit.TestMapper
import failgood.junit.niceString
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.junit.platform.engine.EngineDiscoveryRequest
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
    var silent: Boolean = false

    override fun discover(
        discoveryRequest: EngineDiscoveryRequest,
        uniqueId: UniqueId
    ): TestDescriptor {
        debug =
            discoveryRequest.configurationParameters
                .getBoolean(FailGoodJunitTestEngineConstants.CONFIG_KEY_DEBUG)
                .orElse(false)
        silent =
            discoveryRequest.configurationParameters.getBoolean(CONFIG_KEY_SILENT).orElse(false)
        failureLogger.add("discoveryRequest", discoveryRequest.niceString())

        val runTestFixtures =
            discoveryRequest.configurationParameters
                .getBoolean(FailGoodJunitTestEngineConstants.CONFIG_KEY_RUN_TEST_FIXTURES)
                .orElse(false)
        val suiteAndFilters =
            ContextFinder(runTestFixtures).findContexts(discoveryRequest)
                ?: return EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.DISPLAY_NAME)

        return FailGoodEngineDescriptor(uniqueId, id, suiteAndFilters)
    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailGoodEngineDescriptor) return
        val startedContexts = ConcurrentHashMap.newKeySet<Context>()

        val loggingEngineExecutionListener =
            LoggingEngineExecutionListener(request.engineExecutionListener)
        val listener =
            FailureLoggingEngineExecutionListener(loggingEngineExecutionListener, failureLogger)
        listener.executionStarted(root)
        try {
            val testMapper = TestMapper()
            val results =
                root.suiteAndFilters.suite.run(
                    filter = root.suiteAndFilters.filter,
                    listener = NewExecutionListener(root, listener, startedContexts, testMapper),
                    silent = true
                )
            if (!silent) results.printSummary(true, false)
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
            failureLogger.add("events", loggingEngineExecutionListener.eventsString())
            failureLogger.fail(e)
        } finally {
            if (debug) {
                failureLogger.add("events", loggingEngineExecutionListener.eventsString())
                File(DEBUG_TXT_FILENAME).writeText(failureLogger.envString())
            }
        }
    }

    internal class FailGoodEngineDescriptor(
        uniqueId: UniqueId?,
        displayName: String?,
        val suiteAndFilters: SuiteAndFilters
    ) : EngineDescriptor(uniqueId, displayName)
}
