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

        return FailGoodEngineDescriptor(uniqueId, id, suiteAndFilters).also {
            // add one fake child because idea does not call execute when the test plan is totally empty
            // (but not always, only when running all tests in project (instead of just one module)
            it.addChild(DynamicTestDescriptor(TestPlanNode.Test("test", "test"), it))
            failureLogger.add("Engine Descriptor", it.toString())

            if (debug) {
                // write the debug file here. sometimes execute is just never called
                writeDebugFile()
            }
        }
    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailGoodEngineDescriptor) return
        val startedContexts = ConcurrentHashMap.newKeySet<Context>()

        val loggingEngineExecutionListener =
            LoggingEngineExecutionListener(request.engineExecutionListener)
        val junitListener =
            FailureLoggingEngineExecutionListener(loggingEngineExecutionListener, failureLogger)
        junitListener.executionStarted(root)
        try {
            val testMapper = TestMapper()
            val failgoodListener =
                NewExecutionListener(root, junitListener, startedContexts, testMapper)
            val results =
                root.suiteAndFilters.suite.run(
                    filter = root.suiteAndFilters.filter,
                    listener = failgoodListener,
                    silent = true
                )
            // report the failing root contexts
            results.failedRootContexts.forEach {
                val node = TestPlanNode.Container(it.context.name, it.context.displayName)
                val testDescriptor = DynamicTestDescriptor(node, root)

                junitListener.dynamicTestRegistered(testDescriptor)
                junitListener.executionStarted(testDescriptor)
                junitListener.executionFinished(
                    testDescriptor,
                    TestExecutionResult.failed(it.failure)
                )
            }
            // close all open contexts.
            val leafToRootContexts = startedContexts.sortedBy { -it.parents.size }
            leafToRootContexts.forEach { context ->
                junitListener.executionFinished(
                    testMapper.getMapping(context),
                    TestExecutionResult.successful()
                )
            }
            junitListener.executionFinished(root, TestExecutionResult.successful())
            if (!silent) results.printSummary(printSlowest = true, printPending = false)
        } catch (e: Exception) {
            failureLogger.add("events", loggingEngineExecutionListener.eventsString())
            failureLogger.fail(e)
        } finally {
            if (debug) {
                failureLogger.add("events", loggingEngineExecutionListener.eventsString())
                writeDebugFile()
            }
        }
    }

    private fun writeDebugFile() {
        File(DEBUG_TXT_FILENAME).writeText(failureLogger.envString())
    }

    internal data class FailGoodEngineDescriptor(
        private val uniqueId: UniqueId?,
        private val displayName: String?,
        val suiteAndFilters: SuiteAndFilters
    ) : EngineDescriptor(uniqueId, displayName)
}
