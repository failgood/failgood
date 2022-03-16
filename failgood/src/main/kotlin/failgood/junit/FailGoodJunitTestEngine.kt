package failgood.junit

import failgood.*
import failgood.internal.FailedContext
import failgood.internal.SuiteExecutionContext
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_DEBUG
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_TEST_CLASS_SUFFIX
import failgood.junit.JunitExecutionListener.TestExecutionEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.junit.platform.engine.*
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.exitProcess

const val CONTEXT_SEGMENT_TYPE = "class"
const val TEST_SEGMENT_TYPE = "method"

class FailGoodJunitTestEngine : TestEngine {
    private var debug: Boolean = false
    override fun getId(): String = FailGoodJunitTestEngineConstants.id

    @OptIn(DelicateCoroutinesApi::class)
    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        val startedAt = upt()

        debug = discoveryRequest.configurationParameters.getBoolean(CONFIG_KEY_DEBUG).orElse(false)

        val executionListener = JunitExecutionListener()
        val testSuffix = discoveryRequest.configurationParameters.get(CONFIG_KEY_TEST_CLASS_SUFFIX).orElse("Test")
        val contextsAndFilters = ContextFinder(testSuffix).findContexts(discoveryRequest)
        val providers: List<ContextProvider> = contextsAndFilters.contexts
        if (providers.isEmpty())
        // if we did not find any tests just return an empty descriptor, maybe other engines have tests to run
            return EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.displayName)
        val suite = Suite(providers)
        val suiteExecutionContext = SuiteExecutionContext()
        val testResult = runBlocking(suiteExecutionContext.coroutineDispatcher) {
            val testResult = suite.findTests(
                GlobalScope, true, contextsAndFilters.filter, executionListener
            ).awaitAll()
            val testsCollectedAt = upt()
            println("start: $startedAt tests collected at $testsCollectedAt, discover finished at ${upt()}")
            testResult
        }
        return createResponse(
            uniqueId,
            testResult,
            FailGoodEngineDescriptor(uniqueId, testResult, executionListener, suiteExecutionContext)
        ).also {
            if (debug) {
                println("nodes returned: ${it.allDescendants()}")
            }
        }
    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailGoodEngineDescriptor) return
        val watchdog = System.getenv("FAILGOOD_WATCHDOG_MILLIS")?.let {
            Timer("watchdog", true).schedule(it.toLong()) {
                Thread.getAllStackTraces().forEach { (thread, stackTraceElements) ->
                    println("\n* Thread:${thread.name}: ${stackTraceElements.joinToString<StackTraceElement?>("\n")}")
                }
                exitProcess(-1)
            }
        }

        val suiteExecutionContext = root.suiteExecutionContext
        try {
            if (debug) {
                println("nodes received: ${root.allDescendants()}")
            }
            val mapper = root.mapper
            val startedContexts = mutableSetOf<TestContainer>()
            val junitListener = LoggingEngineExecutionListener(request.engineExecutionListener)
            junitListener.executionStarted(root)
            // report failed contexts as failed immediately
            val failedContexts: MutableList<FailedContext> = root.failedContexts
            failedContexts.forEach {
                val testDescriptor = mapper.getMapping(it.context)
                junitListener.executionStarted(testDescriptor)
                junitListener.executionFinished(testDescriptor, TestExecutionResult.failed(it.failure))
            }
            val executionListener = root.executionListener
            val results = runBlocking(suiteExecutionContext.coroutineDispatcher) {
                // report results while they come in. we use a channel because tests were already running before the execute
                // method was called so when we get here there are probably tests already finished
                val eventForwarder = launch {
                    while (true) {
                        val event = try {
                            executionListener.events.receive()
                        } catch (e: ClosedReceiveChannelException) {
                            break
                        }

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
                            // its a failing root context, so ignore it
                            if (parents.isEmpty()) continue
                            // as a sanity check we try to find the mapping for the parent context, and if that works everything is fine
                            mapper.getMappingOrNull(parents.last())
                                // and if we don't find that maybe the root context of the context is failed
                                ?: if (!failedContexts.any { it.context == parents.first() })
                                    throw FailGoodException("did not find mapping for event $event.")
                            continue
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
                                    is Failed -> {
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

                                    is Pending -> {
                                        withContext(Dispatchers.IO) {
                                            startParentContexts(event.testResult.test)
                                            junitListener.executionSkipped(mapping, "test is skipped")
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
// for debugging println(junitListener.events.joinToString("\n"))

            if (System.getenv("PRINT_SLOWEST") != null) results.printSlowestTests()
            suiteExecutionContext.close()
        } catch (e: Throwable) {
            println(
                "exception occurred inside failgood.\n" + "if you run the latest version please submit a bug at " +
                    "https://github.com/failgood/failgood/issues " +
                    "or tell someone in the #failgood channel in the kotlin-lang slack"
            )
            e.printStackTrace()
            throw e
        } finally {
            watchdog?.cancel()
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
