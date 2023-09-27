package failgood.junit

import failgood.*
import failgood.internal.ExecuteAllTestFilterProvider
import failgood.internal.StaticTestFilterProvider
import failgood.internal.StringListTestFilter
import failgood.internal.SuiteExecutionContext
import failgood.internal.util.getEnv
import failgood.junit.ChannelExecutionListener.TestExecutionEvent
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_DEBUG
import failgood.junit.FailGoodJunitTestEngineConstants.RUN_TEST_FIXTURES
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import org.junit.platform.engine.*
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import java.io.File
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.exitProcess

const val CONTEXT_SEGMENT_TYPE = "class"
const val TEST_SEGMENT_TYPE = "method"

// an optional watchdog that throws an exception when failgood hangs or takes too long
private val watchdogMillis = getEnv("FAILGOOD_WATCHDOG_MILLIS")?.toLong()

private const val DEBUG_TXT_FILENAME = "failgood.debug.txt"

class FailGoodJunitTestEngine : TestEngine {
    private var debug: Boolean = false
    override fun getId(): String = FailGoodJunitTestEngineConstants.id
    private val failureLogger = FailureLogger()

    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        val watchdog = watchdogMillis?.let { Watchdog(it) }
        val startedAt = upt()

        debug = discoveryRequest.configurationParameters.getBoolean(CONFIG_KEY_DEBUG).orElse(false)

        val discoveryRequestToString = discoveryRequestToString(discoveryRequest)
        failureLogger.add("discovery request", discoveryRequestToString)

        try {
            val executionListener = ChannelExecutionListener()
            val runTestFixtures = discoveryRequest.configurationParameters.getBoolean(RUN_TEST_FIXTURES).orElse(false)
            val suiteAndFilters = ContextFinder(runTestFixtures).findContexts(discoveryRequest)

            // if we did not find any tests just return an empty descriptor, maybe other engines have tests to run
            suiteAndFilters ?: return EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.displayName)
            val suite = suiteAndFilters.suite
            val suiteExecutionContext = SuiteExecutionContext()
            val filterProvider = suiteAndFilters.filter ?: getEnv("FAILGOOD_FILTER")
                ?.let { StaticTestFilterProvider(StringListTestFilter(parseFilterString(it))) }
            val testResult = runBlocking(suiteExecutionContext.coroutineDispatcher) {
                val testResult = suite.findTests(
                    suiteExecutionContext.scope,
                    true,
                    filterProvider ?: ExecuteAllTestFilterProvider,
                    executionListener
                ).awaitAll()
                val testsCollectedAt = upt()
                if (debug)
                    println("start: $startedAt tests collected at $testsCollectedAt, discover finished at ${upt()}")
                testResult
            }
            return createResponse(
                uniqueId,
                testResult,
                FailGoodEngineDescriptor(uniqueId, testResult, executionListener, suiteExecutionContext)
            ).also {
                val allDescendants = it.allDescendants()
                failureLogger.add("nodes returned", allDescendants)
            }
        } finally {
            watchdog?.close()
            if (debug) {
                File(DEBUG_TXT_FILENAME).writeText(failureLogger.envString())
            }
        }
    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailGoodEngineDescriptor) return
        val watchdog = watchdogMillis?.let { Watchdog(it) }
        val suiteExecutionContext = root.suiteExecutionContext
        val loggingEngineExecutionListener = LoggingEngineExecutionListener(request.engineExecutionListener)
        try {
            failureLogger.add("nodes received", root.allDescendants())
            failureLogger.add("execute-stacktrace", RuntimeException().stackTraceToString())
            val mapper = root.mapper
            val startedContexts = mutableSetOf<TestContainer>()
            val junitListener = FailureLoggingEngineExecutionListener(
                loggingEngineExecutionListener, failureLogger
            )
            junitListener.executionStarted(root)

            // report failed contexts as failed immediately
            root.failedRootContexts.forEach {
                val testDescriptor = mapper.getMapping(it.context)
                junitListener.executionStarted(testDescriptor)
                junitListener.executionFinished(testDescriptor, TestExecutionResult.failed(it.failure))
            }

            val executionListener = root.executionListener
            val results = runBlocking(suiteExecutionContext.coroutineDispatcher) {
                // report results while they come in. we use a channel because tests were already running before the execute
                // method was called so when we get here there are probably tests already finished
                val eventForwarder = launch {
                    executionListener.events.consumeEach { event ->
                        fun startParentContexts(testDescriptor: TestDescription) {
                            val context = testDescriptor.container
                            (context.parents + context).forEach {
                                if (startedContexts.add(it)) junitListener.executionStarted(mapper.getMapping(it))
                            }
                        }

                        val description = event.testDescription
                        val mapping = mapper.getMappingOrNull(description)
                        // it's possible that we get a test event for a test that has no mapping because it is part of a failing context
                        if (mapping == null) {
                            val parents = description.container.parents
                            val isRootContext = parents.isEmpty()
                            val rootContextOfEvent = if (isRootContext) description.container else parents.first()
                            val isInFailedRootContext = root.failedRootContexts.any {
                                it.context == rootContextOfEvent
                            }
                            if (!isInFailedRootContext)
                                throw FailGoodException("did not find mapping for event $event.")
                            // it's a failing root context, so ignore it
                            return@consumeEach
                        }
                        when (event) {
                            is TestExecutionEvent.Started -> {
                                withContext(Dispatchers.IO) {
                                    startParentContexts(description)
                                    junitListener.executionStarted(mapping)
                                }
                            }

                            is TestExecutionEvent.Stopped -> {
                                val testPlusResult = event.testResult
                                when (testPlusResult.result) {
                                    is Failure -> {
                                        withContext(Dispatchers.IO) {
                                            junitListener.executionFinished(
                                                mapping, TestExecutionResult.failed(testPlusResult.result.failure)
                                            )
                                            junitListener.reportingEntryPublished(
                                                mapping,
                                                ReportEntry.from(
                                                    "uniqueId to rerun just this test", mapping.uniqueId.safeToString()
                                                )
                                            )
                                        }
                                    }

                                    is Success -> withContext(Dispatchers.IO) {
                                        junitListener.executionFinished(
                                            mapping, TestExecutionResult.successful()
                                        )
                                    }

                                    is Skipped -> {
                                        withContext(Dispatchers.IO) {
                                            startParentContexts(event.testResult.test)
                                            junitListener.executionSkipped(mapping, testPlusResult.result.reason)
                                        }
                                    }
                                }
                            }

                            is TestExecutionEvent.TestEvent -> withContext(Dispatchers.IO) {
                                junitListener.reportingEntryPublished(
                                    mapping, ReportEntry.from(event.type, event.payload)
                                )
                            }
                        }
                    }
                }
                // and wait for the results
                val results = awaitTestResults(root.testResult)
                executionListener.events.close()

                // finish forwarding test events before closing all the contexts
                eventForwarder.join()
                results
            }
            // close child contexts before their parent
            val leafToRootContexts = startedContexts.sortedBy { -it.parents.size }
            leafToRootContexts.forEach { context ->
                junitListener.executionFinished(mapper.getMapping(context), TestExecutionResult.successful())
            }

            junitListener.executionFinished(root, TestExecutionResult.successful())

            if (getEnv("PRINT_SLOWEST") != null) results.printSlowestTests()
            suiteExecutionContext.close()
        } catch (e: Throwable) {
            failureLogger.fail(e)
        } finally {
            watchdog?.close()
        }
        if (debug) {
            failureLogger.add("events", loggingEngineExecutionListener.events.toString())
            File(DEBUG_TXT_FILENAME).writeText(failureLogger.envString())
        }
        println("finished after ${uptime()}")
    }
}

class FailGoodTestDescriptor(
    private val type: TestDescriptor.Type,
    id: UniqueId,
    name: String,
    testSource: TestSource? = null
) : AbstractTestDescriptor(id, name, testSource) {
    override fun getType(): TestDescriptor.Type = type
}

private fun UniqueId.safeToString() = toString().replace(" ", "+")
private class Watchdog(timeoutMillis: Long) : AutoCloseable {
    val timer = Timer("watchdog", true)
    val timerTask = timer.schedule(timeoutMillis) {
        Thread.getAllStackTraces().forEach { (thread, stackTraceElements) ->
            println("\n* Thread:${thread.name}: ${stackTraceElements.joinToString<StackTraceElement?>("\n")}")
        }
        exitProcess(-1)
    }

    override fun close() {
        timerTask.cancel()
        timer.cancel()
    }
}

internal fun parseFilterString(filterString: String): List<String> {
    return filterString.split(Regex("[>✔]")).map { it.trim() }
}
