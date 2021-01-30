package failfast.junit

import failfast.ExecutionListener
import failfast.ObjectContextProvider
import failfast.Suite
import failfast.SuiteFailedException
import kotlinx.coroutines.runBlocking
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.EngineDescriptor

internal object FailFastJunitTestEngineConstants {
    const val id = "failfast"
    const val displayName = "FailFast"
}

class FailFastJunitTestEngine : TestEngine {
    override fun getId(): String = FailFastJunitTestEngineConstants.id

    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        return FailFastEngineDescriptor(discoveryRequest, uniqueId)
    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailFastEngineDescriptor)
            return

        val selector = root.discoveryRequest.getSelectorsByType(ClassSelector::class.java).single()
        val context = ObjectContextProvider(selector.javaClass).getContext()
        val listener = request.engineExecutionListener
        listener.executionStarted(root)
        val result = runBlocking {
            Suite(context).run(listener = JunitListenerWrapper(listener))
        }
        result.contexts.forEach {
            listener.dynamicTestRegistered(
                FailFastTestDescriptor(
                    TestDescriptor.Type.CONTAINER,
                    root.uniqueId.append("container", it.toString()),
                    it.toString()
                )
            )
        }
        listener.executionFinished(
            root,
            if (result.allOk) TestExecutionResult.successful() else TestExecutionResult.failed(SuiteFailedException())
        )
    }
}

class FailFastTestDescriptor(private val type: TestDescriptor.Type, id: UniqueId, name: String) :
    AbstractTestDescriptor(id, name) {
    override fun getType(): TestDescriptor.Type {
        return type
    }

}

class JunitListenerWrapper(listener: EngineExecutionListener?) : ExecutionListener {

}

class FailFastEngineDescriptor(val discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId) :
    EngineDescriptor(uniqueId, FailFastJunitTestEngineConstants.displayName)
