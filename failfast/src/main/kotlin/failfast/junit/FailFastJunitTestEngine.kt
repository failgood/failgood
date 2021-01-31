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

private object FailFastJunitTestEngineConstants {
    const val id = "failfast"
    const val displayName = "FailFast"
}

@ExperimentalCoroutinesApi
class FailFastJunitTestEngine : TestEngine {
    override fun getId(): String = FailFastJunitTestEngineConstants.id

    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        val providers: List<ContextProvider> = findContexts(discoveryRequest)
        return runBlocking(Dispatchers.Default) {
            val executionListener = JunitExecutionListener()
            val testResult = Suite(providers).findTests(this, true, executionListener)
            createResponse(uniqueId, testResult, executionListener)
        }
    }

    internal suspend fun createResponse(
        uniqueId: UniqueId,
        testResult: List<Deferred<ContextInfo>>,
        executionListener: JunitExecutionListener
    ): FailFastEngineDescriptor {
        val result = FailFastEngineDescriptor(uniqueId, testResult, executionListener)
        testResult.forEach { defcontext ->
            val contextInfo = defcontext.await()
            val rootContext = contextInfo.contexts.single { it.parent == null }
            val tests = contextInfo.tests.entries
            fun addChildren(node: TestDescriptor, context: Context) {
                val contextNode = FailFastTestDescriptor(
                    TestDescriptor.Type.CONTAINER,
                    uniqueId.append("container", context.toString()),
                    context.name
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

            addChildren(result, rootContext)
        }
        return result
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
        val startedContexts = mutableSetOf<Context>()
        val junitListener = request.engineExecutionListener
        junitListener.executionStarted(root)
        val executionListener = root.executionListener
        var running = true
        val allOk = runBlocking(Dispatchers.Default) {
            // report results while they come in. we use a channel because tests were already running before the execute
            // method was called so when we get here there are probably tests already finished
            launch {
                while (running || !executionListener.started.isEmpty || !executionListener.finished.isEmpty) {
                    select<Unit> {
                        executionListener.started.onReceive {
                            if (startedContexts.add(it.parentContext))
                                junitListener.executionStarted(root.getMapping(it.parentContext))
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
            // and wait for the results
            val allContexts = root.testResult.awaitAll()
            val allTests = allContexts.flatMap { it.tests.values }.awaitAll()
            val contexts = allContexts.flatMap { it.contexts }
            running = false
            contexts.forEach {
                junitListener.executionFinished(root.getMapping(it), TestExecutionResult.successful())
            }
            allTests.all { it is Success }
        }
        junitListener.executionFinished(
            root,
            if (allOk) TestExecutionResult.successful() else TestExecutionResult.failed(SuiteFailedException())
        )
    }

    @OptIn(ExperimentalPathApi::class)
    private fun findContexts(discoveryRequest: EngineDiscoveryRequest): List<ContextProvider> {
        val classPathSelector = discoveryRequest.getSelectorsByType(ClasspathRootSelector::class.java)
            .singleOrNull { !it.classpathRoot.path.contains("resources") }
        val singleClassSelector = discoveryRequest.getSelectorsByType(ClassSelector::class.java).singleOrNull()
        return when {
            classPathSelector != null -> {
                val uri = classPathSelector.classpathRoot
                findClassesInPath(uri.toPath(), Thread.currentThread().contextClassLoader).map {
                    ObjectContextProvider(
                        it
                    )
                }
            }
            singleClassSelector != null -> {
                if (singleClassSelector.className.contains("RunAllTests"))
                    listOf(findTestClasses(randomTestClass = singleClassSelector.javaClass.kotlin))
                listOf(ObjectContextProvider(singleClassSelector.javaClass))
            }
            else -> throw FailFastException("unknown selector in discovery request: $discoveryRequest")
        }
    }
}

private fun TestDescription.toTestDescriptor(uniqueId: UniqueId): TestDescriptor {
    return FailFastTestDescriptor(
        TestDescriptor.Type.TEST,
        uniqueId.append("Test", this.toString()),
        this.testName
    )
}

class FailFastTestDescriptor(
    private val type: TestDescriptor.Type,
    id: UniqueId,
    name: String
) :
    AbstractTestDescriptor(id, name) {
    override fun getType(): TestDescriptor.Type {
        return type
    }

}


@ExperimentalCoroutinesApi
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
