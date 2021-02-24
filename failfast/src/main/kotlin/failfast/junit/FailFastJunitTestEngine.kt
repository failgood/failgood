package failfast.junit

import failfast.Context
import failfast.ContextProvider
import failfast.ExecutionListener
import failfast.FailFast.findClassesInPath
import failfast.FailFastException
import failfast.Failed
import failfast.Ignored
import failfast.ObjectContextProvider
import failfast.Success
import failfast.Suite
import failfast.SuiteFailedException
import failfast.TestDescription
import failfast.TestResult
import failfast.internal.ContextInfo
import failfast.junit.FailFastJunitTestEngine.JunitExecutionListener.StartedOrStopped
import failfast.junit.FailFastJunitTestEngineConstants.CONFIG_KEY_DEBUG
import failfast.uptime
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
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
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSource
import java.io.File
import java.nio.file.Paths
import kotlin.reflect.KClass

object FailFastJunitTestEngineConstants {
    const val id = "failfast"
    const val displayName = "FailFast"
    const val CONFIG_KEY_DEBUG = "failfast.debug"
}

// what idea usually sends:
//selectors:ClasspathRootSelector [classpathRoot = file:///Users/christoph/Projects/mine/failfast/failfast/out/test/classes/], ClasspathRootSelector [classpathRoot = file:///Users/christoph/Projects/mine/failfast/failfast/out/test/resources/]
//filters:IncludeClassNameFilter that includes class names that match one of the following regular expressions: 'failfast\..*', ExcludeClassNameFilter that excludes class names that match one of the following regular expressions: 'com\.intellij\.rt.*' OR 'com\.intellij\.junit3.*'
class FailFastJunitTestEngine : TestEngine {
    private var debug: Boolean = false
    override fun getId(): String = FailFastJunitTestEngineConstants.id

    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        println("starting at uptime ${uptime()}")

        debug = discoveryRequest.configurationParameters.getBoolean(CONFIG_KEY_DEBUG).orElse(false)
        val providers: List<ContextProvider> = findContexts(discoveryRequest)
        return runBlocking(Dispatchers.Default) {
            val executionListener = JunitExecutionListener()
            val testResult = Suite(providers).findTests(this, true, executionListener)
            createResponse(uniqueId, testResult, executionListener)
        }
    }

    private suspend fun createResponse(
        uniqueId: UniqueId,
        contextInfos: List<Deferred<ContextInfo>>,
        executionListener: JunitExecutionListener
    ): FailFastEngineDescriptor {
        val result = FailFastEngineDescriptor(uniqueId, contextInfos, executionListener)
        contextInfos.forEach { deferred ->
            val contextInfo = deferred.await()
            val tests = contextInfo.tests.entries
            fun addChildren(node: TestDescriptor, context: Context) {
                val contextNode = FailFastTestDescriptor(
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
        sealed class StartedOrStopped {
            data class Started(val testDescriptor: TestDescription) : StartedOrStopped()
            data class Stopped(val testResult: TestResult) : StartedOrStopped()

        }

        val events = Channel<StartedOrStopped>(UNLIMITED)
        override suspend fun testStarted(testDescriptor: TestDescription) {
            events.send(StartedOrStopped.Started(testDescriptor))
        }

        override suspend fun testFinished(testDescriptor: TestDescription, testResult: TestResult) {
            events.send(StartedOrStopped.Stopped(testResult))
        }

    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailFastEngineDescriptor)
            return
        val startedContexts = mutableSetOf<Context>()
        val junitListener = request.engineExecutionListener
        junitListener.executionStarted(root)
        val executionListener = root.executionListener
        runBlocking(Dispatchers.Default) {
            // report results while they come in. we use a channel because tests were already running before the execute
            // method was called so when we get here there are probably tests already finished
            launch {
                while (true) {
                    val event = try {
                        executionListener.events.receive()
                    } catch (e: Exception) {
                        break
                    }
                    when (event) {
                        is StartedOrStopped.Started -> {
                            val testDescriptor = event.testDescriptor
                            if (startedContexts.add(testDescriptor.parentContext))
                                junitListener.executionStarted(root.getMapping(testDescriptor.parentContext))
                            junitListener.executionStarted(root.getMapping(testDescriptor))
                        }
                        is StartedOrStopped.Stopped -> {
                            val it = event.testResult
                            val mapping = root.getMapping(it.test)
                            when (it) {
                                is Failed -> junitListener.executionFinished(
                                    mapping,
                                    TestExecutionResult.failed(it.failure)
                                )

                                is Success -> junitListener.executionFinished(
                                    mapping,
                                    TestExecutionResult.successful()
                                )

                                is Ignored -> junitListener.executionSkipped(mapping, null)
                            }

                        }
                    }
                }
            }
            // and wait for the results
            val allContexts = root.testResult.awaitAll()
            val allTests = allContexts.flatMap { it.tests.values }.awaitAll()
            val contexts = allContexts.flatMap { it.contexts }
            executionListener.events.close()
            contexts.forEach { context ->
                junitListener.executionFinished(root.getMapping(context), TestExecutionResult.successful())
            }

            junitListener.executionFinished(
                root,
                if (allTests.all { it is Success }) TestExecutionResult.successful() else TestExecutionResult.failed(
                    SuiteFailedException("test failed")
                )
            )
        }
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
                        matchLambda = { className -> classNamePredicates.all { it.test(className) } }).mapNotNull {
                        contextOrNull(it)
                    }
                }
            }
            classSelectors.isNotEmpty() -> classSelectors.filter { it.className.endsWith("Test") }
                .mapNotNull { contextOrNull(it.javaClass.kotlin) }

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
                throw FailFastException(
                    message
                )
            }
        }
    }

    private fun contextOrNull(it: KClass<*>) = try {
        ObjectContextProvider(
            it
        )
    } catch (e: Exception) {
        null
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
    return FailFastTestDescriptor(
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

class FailFastTestDescriptor(
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


internal class FailFastEngineDescriptor(
    uniqueId: UniqueId,
    val testResult: List<Deferred<ContextInfo>>,
    val executionListener: FailFastJunitTestEngine.JunitExecutionListener
) :
    EngineDescriptor(uniqueId, FailFastJunitTestEngineConstants.displayName) {
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
