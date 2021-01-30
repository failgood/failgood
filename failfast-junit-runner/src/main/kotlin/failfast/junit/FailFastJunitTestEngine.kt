package failfast.junit

import failfast.*
import failfast.FailFast.findClassesInPath
import failfast.FailFast.findTestClasses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.platform.engine.*
import org.junit.platform.engine.TestDescriptor
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
        val result = FailFastEngineDescriptor(discoveryRequest, uniqueId)
        val providers: List<ContextProvider> = findContexts(discoveryRequest)
        runBlocking(Dispatchers.Default) {
            val testResult = Suite(providers).findTests(
                coroutineScope = this,
                executeTests = true
            )
            testResult.forEach { defcontext ->
                val context = defcontext.await()
                val rootContextNode = FailFastTestDescriptor(
                    TestDescriptor.Type.CONTAINER,
                    uniqueId.append("container", context.toString()),
                    context.toString()
                )
                result.addChild(
                    rootContextNode
                )
            }
        }
        return result

    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailFastEngineDescriptor)
            return

        val providers: List<ContextProvider> = findContexts(root.discoveryRequest)

        val listener = request.engineExecutionListener
        listener.executionStarted(root)
        runBlocking {
            val result = Suite(providers).findTests(
                coroutineScope = this,
                executeTests = true
            )
            result.forEach { defcontext ->
                val context = defcontext.await()
                val descriptor = FailFastTestDescriptor(
                    TestDescriptor.Type.CONTAINER,
                    root.uniqueId.append("container", context.toString()),
                    context.toString()
                )
                listener.executionStarted(
                    descriptor
                )
//                listener.executionFinished(descriptor, context)
            }
            listener.executionFinished(
                root,
                if (true) TestExecutionResult.successful() else TestExecutionResult.failed(SuiteFailedException())
            )
        }

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

class FailFastTestDescriptor(private val type: TestDescriptor.Type, id: UniqueId, name: String) :
    AbstractTestDescriptor(id, name) {
    override fun getType(): TestDescriptor.Type {
        return type
    }

}

class JunitListenerWrapper(val listener: EngineExecutionListener) : ExecutionListener {

}

class FailFastEngineDescriptor(val discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId) :
    EngineDescriptor(uniqueId, FailFastJunitTestEngineConstants.displayName)
