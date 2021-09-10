package failgood.junit

import failgood.ContextProvider
import failgood.Failed
import failgood.Pending
import failgood.Success
import failgood.Suite
import failgood.TestContainer
import failgood.TestDescription
import failgood.internal.ContextInfo
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_DEBUG
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_LAZY
import failgood.junit.JunitExecutionListener.TestExecutionEvent
import failgood.upt
import failgood.uptime
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor

const val CONTEXT_SEGMENT_TYPE = "class"
const val TEST_SEGMENT_TYPE = "method"

/*
junit support is very hacky currently, mostly because it has no tests.
nowadays, it works so well that I will probably have to refactor and test it.
 */

class FailGoodJunitTestEngine : TestEngine {
    private var debug: Boolean = false
    override fun getId(): String = FailGoodJunitTestEngineConstants.id

    @OptIn(DelicateCoroutinesApi::class)
    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        val lazy = discoveryRequest.configurationParameters.getBoolean(CONFIG_KEY_LAZY).orElse(false)
        println("starting at uptime ${uptime()}" + if (lazy) " lazy mode enabled" else "")

        debug = discoveryRequest.configurationParameters.getBoolean(CONFIG_KEY_DEBUG).orElse(false)

        return runBlocking(Dispatchers.Default) {
            val providers: List<ContextProvider> = findContexts(discoveryRequest)
            val suite = Suite(providers)
            val executionListener = JunitExecutionListener()

            val testResult = suite.findTests(GlobalScope, !lazy, executionListener).awaitAll()
            println("test results collected at ${upt()}")
            @Suppress("DeferredResultUnused")
            if (lazy)
                GlobalScope.async(Dispatchers.Default) {
                    testResult.filterIsInstance<ContextInfo>().map { it.tests.values.awaitAll() }
                }
            createResponse(
                uniqueId,
                testResult,
                executionListener
            ).also { println("discover finished at uptime ${upt()}") }
        }
    }


    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailGoodEngineDescriptor)
            return
        val mapper = root.mapper
        val startedContexts = mutableSetOf<TestContainer>()
        val junitListener = LoggingEngineExecutionListener(request.engineExecutionListener)
        junitListener.executionStarted(root)
        // report failed contexts as failed immediately
        root.failedContexts.forEach {
            junitListener.executionFinished(mapper.getMapping(it.context), TestExecutionResult.failed(it.failure))
        }
        val executionListener = root.executionListener
        runBlocking(Dispatchers.Default) {
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
                    val mapping = mapper.getMapping(description)
                    when (event) {
                        is TestExecutionEvent.Started -> {
                            startParentContexts(description)
                            junitListener.executionStarted(mapping)
                        }
                        is TestExecutionEvent.Stopped -> {
                            val testPlusResult = event.testResult
                            when (testPlusResult.result) {
                                is Failed -> junitListener.executionFinished(
                                    mapping,
                                    TestExecutionResult.failed(testPlusResult.result.failure)
                                )

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
            // and wait for the results
            val succesfulContexts = root.testResult.filterIsInstance<ContextInfo>()
            succesfulContexts.flatMap { it.tests.values }.awaitAll()
            executionListener.events.close()

            // finish forwarding test events before closing all the contexts
            eventForwarder.join()
            // close child contexts before their parent
            val leafToRootContexts = startedContexts.sortedBy { -it.parents.size }
            leafToRootContexts.forEach { context ->
                junitListener.executionFinished(mapper.getMapping(context), TestExecutionResult.successful())
            }

            junitListener.executionFinished(root, TestExecutionResult.successful())
        }
//        println(junitListener.events.joinToString("\n"))
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


