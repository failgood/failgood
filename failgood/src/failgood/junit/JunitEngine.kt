package failgood.junit

import failgood.Context
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_PARALLELISM
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_SILENT
import failgood.junit.FailGoodJunitTestEngineConstants.DEBUG_TXT_FILENAME
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClasspathRootSelector
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

class JunitEngine : TestEngine {
    override fun getId(): String = FailGoodJunitTestEngineConstants.ID

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
            // Add one fake child because IDEA does not call `execute` when the test plan is totally empty.
            // This does not happen always but only when running "all tests in Project" (instead of just one module)
            // We try to detect this case and add a fake test
            val classPathRootSelectors = discoveryRequest.getSelectorsByType(ClasspathRootSelector::class.java)
            // idea adds classpath roots for kotlin, java and resources, so for one root it adds 3
            if (classPathRootSelectors.size > 3)
                it.addChild(DynamicTestDescriptor(TestPlanNode.Test("test", "empty-test-please-ignore"), it))
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
                ExecutionListener(root, junitListener, startedContexts, testMapper)
            val results =
                root.suiteAndFilters.suite.run(
                    parallelism = request.configurationParameters.get(CONFIG_KEY_PARALLELISM).getOrNull()?.toInt(),
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
