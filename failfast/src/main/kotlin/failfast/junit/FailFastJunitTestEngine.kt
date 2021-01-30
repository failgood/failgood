package failfast.junit

import failfast.*
import failfast.FailFast.findClassesInPath
import failfast.FailFast.findTestClasses
import failfast.internal.ContextInfo
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
            val testResult = Suite(providers).findTests(
                coroutineScope = this,
                executeTests = true
            )
            val result = FailFastEngineDescriptor(discoveryRequest, uniqueId, testResult)
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
                    testsInThisContext.forEach { contextNode.addChild(it.key.toTestDescriptor(uniqueId, it.value)) }
                    val contextsInThisContext = contextInfo.contexts.filter { it.parent == context }
                    contextsInThisContext.forEach { addChildren(contextNode, it) }
                    node.addChild(contextNode)
                }

                addChildren(result, rootContext)
            }
            result
        }
    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailFastEngineDescriptor)
            return

        val listener = request.engineExecutionListener
        listener.executionStarted(root)
        listener.executionFinished(root, TestExecutionResult.successful())
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

private fun TestDescription.toTestDescriptor(uniqueId: UniqueId, value: Deferred<TestResult>): TestDescriptor {
    return FailFastTestDescriptor(
        TestDescriptor.Type.TEST,
        uniqueId.append("Test", this.testName),
        this.testName,
        value
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

class JunitListenerWrapper(val listener: EngineExecutionListener) : ExecutionListener {

}

internal class FailFastEngineDescriptor(
    val discoveryRequest: EngineDiscoveryRequest,
    uniqueId: UniqueId,
    val testResult: List<Deferred<ContextInfo>>
) :
    EngineDescriptor(uniqueId, FailFastJunitTestEngineConstants.displayName)
