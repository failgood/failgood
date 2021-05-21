package failgood.junit

import failgood.*
import failgood.FailGood.findClassesInPath
import failgood.internal.ContextInfo
import failgood.junit.FailGoodJunitTestEngine.JunitExecutionListener.TestExecutionEvent
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_DEBUG
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_LAZY
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassNameFilter
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.ClasspathRootSelector
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.engine.support.descriptor.*
import java.io.File
import java.nio.file.Paths

object FailGoodJunitTestEngineConstants {
    const val id = "failgood"
    const val displayName = "FailGood"
    const val CONFIG_KEY_DEBUG = "failgood.debug"
    const val CONFIG_KEY_LAZY = "failgood.lazy"
}

// what idea usually sends:
//selectors:ClasspathRootSelector [classpathRoot = file:///Users/christoph/Projects/mine/failgood/failgood/out/test/classes/], ClasspathRootSelector [classpathRoot = file:///Users/christoph/Projects/mine/failgood/failgood/out/test/resources/]
//filters:IncludeClassNameFilter that includes class names that match one of the following regular expressions: 'failgood\..*', ExcludeClassNameFilter that excludes class names that match one of the following regular expressions: 'com\.intellij\.rt.*' OR 'com\.intellij\.junit3.*'
class FailGoodJunitTestEngine : TestEngine {
    private var debug: Boolean = false
    override fun getId(): String = FailGoodJunitTestEngineConstants.id

    @OptIn(DelicateCoroutinesApi::class)
    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        println("starting at uptime ${uptime()}")

        debug = discoveryRequest.configurationParameters.getBoolean(CONFIG_KEY_DEBUG).orElse(false)
        val lazy = discoveryRequest.configurationParameters.getBoolean(CONFIG_KEY_LAZY).orElse(false)
        val providers: List<ContextProvider> = findContexts(discoveryRequest)
        val suite = Suite(providers)
        val executionListener = JunitExecutionListener()

        return runBlocking(Dispatchers.Default) {

            val testResult = suite.findTests(GlobalScope, !lazy, executionListener).awaitAll()
            @Suppress("DeferredResultUnused")
            if (lazy)
                GlobalScope.async(Dispatchers.Default) { testResult.map { it.tests.values.awaitAll() } }
            createResponse(uniqueId, testResult, executionListener)
        }
    }

    private fun createResponse(
        uniqueId: UniqueId,
        contextInfos: List<ContextInfo>,
        executionListener: JunitExecutionListener
    ): FailGoodEngineDescriptor {
        val result = FailGoodEngineDescriptor(uniqueId, contextInfos, executionListener)
        contextInfos.forEach { contextInfo ->

            val tests = contextInfo.tests.entries
            fun addChildren(node: TestDescriptor, context: Context) {
                val contextNode = FailGoodTestDescriptor(
                    TestDescriptor.Type.CONTAINER,
                    uniqueId.append("container", context.stringPath()),
                    context.name,
                    context.stackTraceElement?.let { createFileSource(it) }
                )
                result.addMapping(context, contextNode)
                val testsInThisContext = tests.filter { it.key.parentContext == context }
                testsInThisContext.forEach {
                    val testDescription = it.key
                    val testDescriptor = testDescription.toTestDescriptor(uniqueId)
                    contextNode.addChild(testDescriptor)
                    result.addMapping(testDescription, testDescriptor)
                }
                val contextsInThisContext = contextInfo.contexts.filter { it.parent == context }
                contextsInThisContext.forEach { addChildren(contextNode, it) }
                node.addChild(contextNode)
            }

            val rootContext = contextInfo.contexts.singleOrNull { it.parent == null }
            rootContext?.let { addChildren(result, it) }
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
        val startedContexts = mutableSetOf<Context>()
        val junitListener = LoggingEngineExecutionListener(request.engineExecutionListener)
        junitListener.executionStarted(root)
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
                        val context = testDescriptor.parentContext
                        (context.parentContexts + context).forEach {
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
            val allContexts = root.testResult
            allContexts.flatMap { it.tests.values }.awaitAll()
            executionListener.events.close()

            // finish forwarding test events before closing all the contexts
            eventForwarder.join()
            // close child contexts before their parent
            val leafToRootContexts = startedContexts.sortedBy { -it.parentContexts.size }
            leafToRootContexts.forEach { context ->
                junitListener.executionFinished(root.getMapping(context), TestExecutionResult.successful())
            }

            junitListener.executionFinished(root, TestExecutionResult.successful())
        }
//        println(junitListener.events.joinToString("\n"))
        println("finished after ${uptime()}")
    }

    private fun findContexts(discoveryRequest: EngineDiscoveryRequest): List<ContextProvider> {
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
    val testSource =
        createFileSource(stackTraceElement)
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


internal class FailGoodEngineDescriptor(
    uniqueId: UniqueId,
    val testResult: List<ContextInfo>,
    val executionListener: FailGoodJunitTestEngine.JunitExecutionListener
) :
    EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.displayName) {
    private val testDescription2JunitTestDescriptor = mutableMapOf<TestDescription, TestDescriptor>()
    private val context2JunitTestDescriptor = mutableMapOf<Context, TestDescriptor>()
    fun addMapping(testDescription: TestDescription, testDescriptor: TestDescriptor) {
        testDescription2JunitTestDescriptor[testDescription] = testDescriptor
    }

    fun getMapping(testDescription: TestDescription) = testDescription2JunitTestDescriptor[testDescription]
    fun getMapping(context: Context): TestDescriptor = context2JunitTestDescriptor[context]!!
    fun addMapping(context: Context, testDescriptor: TestDescriptor) {
        context2JunitTestDescriptor[context] = testDescriptor
    }
}
