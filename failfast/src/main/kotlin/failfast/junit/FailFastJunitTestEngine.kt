package failfast.junit

import failfast.*
import failfast.FailFast.findClassesInPath
import failfast.FailFast.findTestClasses
import failfast.internal.ContextInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.selects.select
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.ClasspathRootSelector
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.toPath

internal object FailFastJunitTestEngineConstants {
    const val id = "failfast"
    const val displayName = "FailFast"
}

class FailFastJunitTestEngine : TestEngine {
    override fun getId(): String = FailFastJunitTestEngineConstants.id

    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        val providers: List<ContextProvider> = findContexts(discoveryRequest)
        return runBlocking(Dispatchers.Default) {
            val executionListener = JunitExecutionListener()
            val testResult = Suite(providers).findTests(this, true, executionListener)
            val result = FailFastEngineDescriptor(discoveryRequest, uniqueId, testResult, executionListener)
            testResult.forEach { defcontext ->
                val contextInfo = defcontext.await()
                val rootContext = contextInfo.contexts.single { it.parent == null }
                val tests = contextInfo.tests.entries
                fun addChildren(node: TestDescriptor, context: Context) {
                    val contextNode = FailFastTestDescriptor(
                        TestDescriptor.Type.CONTAINER,
                        uniqueId.append("container", context.name),
                        context.name
                    )
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

                addChildren(result, rootContext)
            }
            result
        }
    }

    class JunitExecutionListener : ExecutionListener {
        val started = Channel<TestDescription>(UNLIMITED)
        val finished = Channel<TestResult>(UNLIMITED)
        override suspend fun testStarted(testDescriptor: TestDescription) {
            started.send(testDescriptor)
        }

        override suspend fun testFinished(testDescriptor: TestDescription, testResult: TestResult) {
            finished.send(testResult)
        }

    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailFastEngineDescriptor)
            return

        val junitListener = request.engineExecutionListener
        junitListener.executionStarted(root)
        val executionListener = root.executionListener
        var running = true
        runBlocking(Dispatchers.Default) {
            launch {
                while (running || !executionListener.started.isEmpty || !executionListener.finished.isEmpty) {
                    select<Unit> {
                        executionListener.started.onReceive {
                            junitListener.executionStarted(root.getMapping(it))
                        }
                        executionListener.finished.onReceive {
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
            root.testResult.awaitAll().flatMap<ContextInfo, Deferred<TestResult>> { it.tests.values }.awaitAll()
            running = false

        }
        junitListener.executionFinished(root, TestExecutionResult.successful())
    }

    @OptIn(ExperimentalPathApi::class)
    private fun findContexts(discoveryRequest: EngineDiscoveryRequest): List<ContextProvider> {
        val classPathSelector = discoveryRequest.getSelectorsByType(ClasspathRootSelector::class.java).singleOrNull()
        val singleClassSelector = discoveryRequest.getSelectorsByType(ClassSelector::class.java).singleOrNull()
        val providers: List<ContextProvider> = if (classPathSelector != null) {
            val uri = classPathSelector.classpathRoot
            findClassesInPath(uri.toPath(), Thread.currentThread().contextClassLoader).map { ObjectContextProvider(it) }
        } else if (singleClassSelector != null) {
            if (singleClassSelector.className.contains("RunAllTests"))
                listOf(findTestClasses(randomTestClass = singleClassSelector.javaClass.kotlin))
            listOf(ObjectContextProvider(singleClassSelector.javaClass))
        } else
            throw RuntimeException()
        return providers
    }
}

private fun TestDescription.toTestDescriptor(uniqueId: UniqueId): TestDescriptor {
    return FailFastTestDescriptor(
        TestDescriptor.Type.TEST,
        uniqueId.append("Test", this.testName),
        this.testName
    )
}

class FailFastTestDescriptor(
    private val type: TestDescriptor.Type,
    id: UniqueId,
    name: String,
    val result: Deferred<TestResult>? = null
) :
    AbstractTestDescriptor(id, name) {
    override fun getType(): TestDescriptor.Type {
        return type
    }

}


internal class FailFastEngineDescriptor(
    val discoveryRequest: EngineDiscoveryRequest,
    uniqueId: UniqueId,
    val testResult: List<Deferred<ContextInfo>>,
    val executionListener: FailFastJunitTestEngine.JunitExecutionListener
) :
    EngineDescriptor(uniqueId, FailFastJunitTestEngineConstants.displayName) {
    private val testDescription2JunitUniqueId = mutableMapOf<TestDescription, TestDescriptor>()
    fun addMapping(testDescription: TestDescription, testDescriptor: TestDescriptor) {
        testDescription2JunitUniqueId[testDescription] = testDescriptor
    }

    fun getMapping(testDescription: TestDescription) = testDescription2JunitUniqueId[testDescription]
}
