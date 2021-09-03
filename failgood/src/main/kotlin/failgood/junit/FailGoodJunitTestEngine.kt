package failgood.junit

import failgood.Context
import failgood.ContextProvider
import failgood.ExecutionListener
import failgood.FailGood.findClassesInPath
import failgood.FailGoodException
import failgood.Failed
import failgood.ObjectContextProvider
import failgood.Pending
import failgood.Success
import failgood.Suite
import failgood.TestContainer
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.internal.ContextInfo
import failgood.internal.ContextResult
import failgood.internal.FailedContext
import failgood.junit.FailGoodJunitTestEngine.JunitExecutionListener.TestExecutionEvent
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_DEBUG
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_LAZY
import failgood.upt
import failgood.uptime
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.platform.engine.DiscoveryFilter
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.ClassNameFilter
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.ClasspathRootSelector
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSource
import java.io.File
import java.nio.file.Paths

/*
val logFile = File("failgood-junit.log").bufferedWriter()
fun println(body: String) {
    logFile.write(body+"\n")
    logFile.flush()
}

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

    private fun createResponse(
        uniqueId: UniqueId,
        contextInfos: List<ContextResult>,
        executionListener: JunitExecutionListener
    ): FailGoodEngineDescriptor {
        val result = FailGoodEngineDescriptor(uniqueId, contextInfos, executionListener)
        contextInfos.forEach { contextInfo ->

            when (contextInfo) {
                is ContextInfo -> {
                    val tests = contextInfo.tests.entries
                    fun addChildren(node: TestDescriptor, context: Context, isRootContext: Boolean) {
                        val contextNode = FailGoodTestDescriptor(
                            TestDescriptor.Type.CONTAINER,
                            uniqueId.append("container", context.stringPath() + context.uuid.toString()),
                            context.name,
                            context.stackTraceElement?.let {
                                if (isRootContext)
                                    createClassSource(it)
                                else
                                    createFileSource(it)
                            }
                        )
                        result.addMapping(context, contextNode)
                        val testsInThisContext = tests.filter { it.key.container == context }
                        testsInThisContext.forEach {
                            val testDescription = it.key
                            val testDescriptor = testDescription.toTestDescriptor(uniqueId)
                            contextNode.addChild(testDescriptor)
                            result.addMapping(testDescription, testDescriptor)
                        }
                        val contextsInThisContext = contextInfo.contexts.filter { it.parent == context }
                        contextsInThisContext.forEach { addChildren(contextNode, it, false) }
                        node.addChild(contextNode)
                    }

                    val rootContext = contextInfo.contexts.singleOrNull { it.parent == null }
                    rootContext?.let { addChildren(result, it, true) }

                }
                is FailedContext -> {
                    val context = contextInfo.context
                    val testDescriptor = FailGoodTestDescriptor(TestDescriptor.Type.CONTAINER,
                        uniqueId.append("container", context.stringPath() + context.uuid.toString()),
                        context.name, context.stackTraceElement?.let { createFileSource(it) })
                    result.addChild(testDescriptor)
                    result.addMapping(context, testDescriptor)
                    result.failedContexts.add(contextInfo)
                }
            }
        }
        return result
    }

    class JunitExecutionListener : ExecutionListener {
        sealed class TestExecutionEvent {
            abstract val testDescription: TestDescription

            data class Started(override val testDescription: TestDescription) : TestExecutionEvent()
            data class Stopped(override val testDescription: TestDescription, val testResult: TestPlusResult) :
                TestExecutionEvent()

            data class TestEvent(override val testDescription: TestDescription, val type: String, val payload: String) :
                TestExecutionEvent()
        }

        val events = Channel<TestExecutionEvent>(UNLIMITED)
        override suspend fun testStarted(testDescription: TestDescription) {
            events.send(TestExecutionEvent.Started(testDescription))
        }

        override suspend fun testFinished(testPlusResult: TestPlusResult) {
            events.send(TestExecutionEvent.Stopped(testPlusResult.test, testPlusResult))
        }

        override suspend fun testEvent(testDescription: TestDescription, type: String, payload: String) {
            events.send(TestExecutionEvent.TestEvent(testDescription, type, payload))
        }

    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailGoodEngineDescriptor)
            return
        val startedContexts = mutableSetOf<TestContainer>()
        val junitListener = LoggingEngineExecutionListener(request.engineExecutionListener)
        junitListener.executionStarted(root)
        root.failedContexts.forEach {
            junitListener.executionFinished(
                root.getMapping(it.context),
                TestExecutionResult.failed(it.failure)
            )
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

                    fun startParentContexts(
                        testDescriptor: TestDescription,
                        engineDescriptor: FailGoodEngineDescriptor
                    ) {
                        val context = testDescriptor.container
                        (context.parents + context).forEach {
                            if (startedContexts.add(it))
                                junitListener.executionStarted(engineDescriptor.getMapping(it))
                        }
                    }

                    val description = event.testDescription
                    val mapping = root.getMapping(description)
                    when (event) {
                        is TestExecutionEvent.Started -> {
                            startParentContexts(description, root)
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
                                    startParentContexts(event.testResult.test, root)
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
                junitListener.executionFinished(root.getMapping(context), TestExecutionResult.successful())
            }

            junitListener.executionFinished(root, TestExecutionResult.successful())
        }
//        println(junitListener.events.joinToString("\n"))
        println("finished after ${uptime()}")
    }

    private suspend fun findContexts(discoveryRequest: EngineDiscoveryRequest): List<ContextProvider> {
        if (debug) {
            println(discoveryRequestToString(discoveryRequest))
        }

        // idea usually sends a classpath selector
        val classPathSelectors = discoveryRequest.getSelectorsByType(ClasspathRootSelector::class.java)

        // gradle sends a class selector for each class
        val classSelectors = discoveryRequest.getSelectorsByType(ClassSelector::class.java)
        val singleClassSelector = discoveryRequest.getSelectorsByType(ClassSelector::class.java).singleOrNull()
        val classNamePredicates =
            discoveryRequest.getFiltersByType(ClassNameFilter::class.java).map { it.toPredicate() }
        return when {
            classPathSelectors.isNotEmpty() -> {
                return classPathSelectors.flatMap { classPathSelector ->
                    val uri = classPathSelector.classpathRoot
                    findClassesInPath(
                        Paths.get(uri),
                        Thread.currentThread().contextClassLoader,
                        matchLambda = { className -> classNamePredicates.all { it.test(className) } }).map {
                        ObjectContextProvider(it)
                    }
                }
            }
            classSelectors.isNotEmpty() -> {
                val classes =
                    if (classSelectors.size == 1) classSelectors else classSelectors.filter { it.className.endsWith("Test") }
                classes
                    .map { ObjectContextProvider(it.javaClass.kotlin) }
            }

            singleClassSelector != null -> {
                listOf(ObjectContextProvider(singleClassSelector.javaClass))
            }
            else -> {
                val message = "unknown selector in discovery request: ${
                    discoveryRequestToString(
                        discoveryRequest
                    )
                }"
                System.err.println(message)
                throw FailGoodException(
                    message
                )
            }
        }
    }

    private fun discoveryRequestToString(discoveryRequest: EngineDiscoveryRequest): String {
        val allSelectors = discoveryRequest.getSelectorsByType(DiscoverySelector::class.java)
        val allFilters = discoveryRequest.getFiltersByType(DiscoveryFilter::class.java)
        return "selectors:${allSelectors.joinToString()}\nfilters:${allFilters.joinToString()}"
    }
}

private fun TestDescription.toTestDescriptor(uniqueId: UniqueId): TestDescriptor {
    val stackTraceElement = this.stackTraceElement
    val testSource = createFileSource(stackTraceElement)
    return FailGoodTestDescriptor(
        TestDescriptor.Type.TEST,
        uniqueId.append("Test", this.toString()),
        this.testName,
        testSource
    )
}

private fun createFileSource(stackTraceElement: StackTraceElement): TestSource? {
    val className = stackTraceElement.className
    val filePosition = FilePosition.from(stackTraceElement.lineNumber)
    val file = File("src/test/kotlin/${className.substringBefore("$").replace(".", "/")}.kt")
    return if (file.exists())
        FileSource.from(
            file,
            filePosition
        )
    else ClassSource.from(className, filePosition)
}

private fun createClassSource(stackTraceElement: StackTraceElement): TestSource? {
    val className = stackTraceElement.className
    val filePosition = FilePosition.from(stackTraceElement.lineNumber)
    return ClassSource.from(className, filePosition)
}

class FailGoodTestDescriptor(
    private val type: TestDescriptor.Type,
    id: UniqueId,
    name: String,
    testSource: TestSource? = null
) :
    AbstractTestDescriptor(id, name, testSource) {
    override fun getType(): TestDescriptor.Type {
        return type
    }

}


