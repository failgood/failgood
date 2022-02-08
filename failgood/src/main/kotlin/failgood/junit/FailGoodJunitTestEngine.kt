package failgood.junit

import failgood.*
import failgood.internal.ContextInfo
import failgood.internal.FailedContext
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_DEBUG
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_LAZY
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_TEST_CLASS_SUFFIX
import failgood.junit.JunitExecutionListener.TestExecutionEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.junit.platform.engine.*
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.EngineDescriptor

const val CONTEXT_SEGMENT_TYPE = "class"
const val TEST_SEGMENT_TYPE = "method"

class FailGoodJunitTestEngine : TestEngine {
    private var debug: Boolean = false
    override fun getId(): String = FailGoodJunitTestEngineConstants.id

    @OptIn(DelicateCoroutinesApi::class)
    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        val lazy = discoveryRequest.configurationParameters.getBoolean(CONFIG_KEY_LAZY).orElse(false)
        println("starting at uptime ${uptime()}" + if (lazy) " lazy mode enabled" else "")

        debug = discoveryRequest.configurationParameters.getBoolean(CONFIG_KEY_DEBUG).orElse(false)

        val executionListener = JunitExecutionListener()
        val testSuffix = discoveryRequest.configurationParameters.get(CONFIG_KEY_TEST_CLASS_SUFFIX).orElse("Test")
        val contextsAndFilters = ContextFinder(testSuffix).findContexts(discoveryRequest)
        val providers: List<ContextProvider> = contextsAndFilters.contexts
        if (providers.isEmpty())
        // if we did not find any tests just return an empty descriptor, maybe other engines have tests to run
            return EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.displayName)
        val suite = Suite(providers)
        val testResult = runBlocking(Dispatchers.Default) {
            val testResult = suite.findTests(
                GlobalScope,
                !lazy,
                listener = executionListener,
                executionFilter = contextsAndFilters.filter
            ).map { it.result }.awaitAll()
            println("test results collected at ${upt()}")
            @Suppress("DeferredResultUnused")
            if (lazy)
                GlobalScope.async(Dispatchers.Default) {
                    testResult.filterIsInstance<ContextInfo>().map { it.tests.values.awaitAll() }
                }
            println("discover finished at uptime ${upt()}")
            testResult
        }
        return createResponse(uniqueId, testResult, executionListener).also {
            println("returning result after ${upt()}")
            if (debug) {
                println("nodes returned: ${it.allDescendants()}")
            }
        }
    }

    override fun execute(request: ExecutionRequest) {
        try {
            val failedTests = mutableListOf<TestDescription>()
            val root = request.rootTestDescriptor
            if (root !is FailGoodEngineDescriptor)
                return
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
            val results = runBlocking(Dispatchers.Default) {
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
                                if (startedContexts.add(it))
                                    junitListener.executionStarted(mapper.getMapping(it))
                            }
                        }

                        val description = event.testDescription
                        val mapping = mapper.getMappingOrNull(description)
                        // it's possible that we get a test event for a test that has no mapping because it is part of a failing context
                        if (mapping == null) {
                            val parents = description.container.parents
                            // its a failing root context, so ignore it
                            if (parents.isEmpty())
                                continue
                            // as a sanity check we try to find the mapping for the parent context, and if that works everything is fine
                            mapper.getMappingOrNull(parents.last())
                                // and if we don't find that maybe the root context of the context is failed
                                ?: if (!failedContexts.any { it.context == parents.first() })
                                    throw FailGoodException("did not find mapping for event $event.")
                            continue
                        }
                        when (event) {
                            is TestExecutionEvent.Started -> {
                                startParentContexts(description)
                                junitListener.executionStarted(mapping)
                            }
                            is TestExecutionEvent.Stopped -> {
                                val testPlusResult = event.testResult
                                when (testPlusResult.result) {
                                    is Failed -> {
                                        junitListener.executionFinished(
                                            mapping,
                                            TestExecutionResult.failed(testPlusResult.result.failure)
                                        )
                                        failedTests.add(description)
                                    }

                                    is Success -> junitListener.executionFinished(
                                        mapping,
                                        TestExecutionResult.successful()
                                    )

                                    is Pending -> {
                                        startParentContexts(event.testResult.test)
                                        junitListener.executionSkipped(mapping, "test is skipped")
                                    }
                                }
                            }
                            is TestExecutionEvent.TestEvent -> junitListener.reportingEntryPublished(
                                mapping,
                                ReportEntry.from(event.type, event.payload)
                            )
                        }
                    }
                }
//            printResults(this, )
                // and wait for the results
                val results = awaitContexts(root.testResult)
                executionListener.events.close()

                // finish forwarding test events before closing all the contexts
                eventForwarder.join()
                // close child contexts before their parent
                val leafToRootContexts = startedContexts.sortedBy { -it.parents.size }
                leafToRootContexts.forEach { context ->
                    junitListener.executionFinished(mapper.getMapping(context), TestExecutionResult.successful())
                }

                junitListener.executionFinished(root, TestExecutionResult.successful())
                results
            }
// for debugging        println(junitListener.events.joinToString("\n"))

            if (System.getenv("PRINT_SLOWEST") != null)
                results.printSlowestTests()

            if (failedTests.isNotEmpty()) {
                println("failed tests with uniqueId to run from IDEA:")
                failedTests.forEach {
                    println(
                        "${it.testName} ${
                        mapper.getMapping(it).uniqueId.toString().replace(" ", "+")
                        }"
                    )
                }
            }
        } catch (e: Exception) {
            println(
                "exception occurred inside failgood.\n" +
                    "if you run the latest version please submit a bug at https://github.com/failgood/failgood/issues"
            )
            e.printStackTrace()
            throw e
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
