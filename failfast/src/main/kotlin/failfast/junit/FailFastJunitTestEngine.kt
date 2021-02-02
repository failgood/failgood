package failfast.junit

import failfast.*
import failfast.FailFast.findClassesInPath
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
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSource
import java.io.File
import java.nio.file.Paths

private object FailFastJunitTestEngineConstants {
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
            createResponse(uniqueId, testResult, executionListener)
        }
    }

    private suspend fun createResponse(
        uniqueId: UniqueId,
        testResult: List<Deferred<ContextInfo>>,
        executionListener: JunitExecutionListener
    ): FailFastEngineDescriptor {
        val result = FailFastEngineDescriptor(uniqueId, testResult, executionListener)
        testResult.forEach { deferred ->
            val contextInfo = deferred.await()
            val rootContext = contextInfo.contexts.single { it.parent == null }
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

    @ExperimentalCoroutinesApi
    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailFastEngineDescriptor)
            return
        val startedContexts = mutableSetOf<Context>()
        val junitListener = request.engineExecutionListener
        junitListener.executionStarted(root)
        val executionListener = root.executionListener
        var running = true
        runBlocking(Dispatchers.Default) {
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
            contexts.forEach { context ->
                val testsInThisContext =
                    allTests.filter { it.test.parentContext.parentContexts.contains(context) || it.test.parentContext == context }
                val failedTests = testsInThisContext.filter { it is Failed }
                val testResult = if (failedTests.isEmpty()) {
                    TestExecutionResult.successful()
                } else {
                    TestExecutionResult.failed(SuiteFailedException("context " + context.stringPath() +
                            " failed because ${failedTests.joinToString { it.test.testName }} failed"
                    )
                    )
                }
                junitListener.executionFinished(root.getMapping(context), testResult)
            }

            junitListener.executionFinished(
                root,
                if (allTests.all { it is Success }) TestExecutionResult.successful() else TestExecutionResult.failed(
                    SuiteFailedException("test failed")
                )
            )
        }
    }

    private fun findContexts(discoveryRequest: EngineDiscoveryRequest): List<ContextProvider> {
        val classPathSelector = discoveryRequest.getSelectorsByType(ClasspathRootSelector::class.java)
            .singleOrNull { !it.classpathRoot.path.contains("resources") }
        val singleClassSelector = discoveryRequest.getSelectorsByType(ClassSelector::class.java).singleOrNull()
        return when {
            classPathSelector != null -> {
                val uri = classPathSelector.classpathRoot
                findClassesInPath(Paths.get(uri), Thread.currentThread().contextClassLoader).mapNotNull {
                    try {
                        ObjectContextProvider(
                            it
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            singleClassSelector != null -> {
                listOf(ObjectContextProvider(singleClassSelector.javaClass))
            }
            else -> throw FailFastException("unknown selector in discovery request: $discoveryRequest")
        }
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

private fun createFileSource(stackTraceElement: StackTraceElement): FileSource? {
    val pathname = "src/test/kotlin/${stackTraceElement.className.substringBefore("$").replace(".", "/")}.kt"
    return FileSource.from(
        File(pathname),
        FilePosition.from(stackTraceElement.lineNumber)
    )
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
