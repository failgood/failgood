package failgood.junit.exp

import failgood.junit.next.DynamicTestDescriptor
import failgood.junit.next.TestPlanNode
import java.util.Optional
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.TestTag
import org.junit.platform.engine.UniqueId

/** this is just to play around with the junit engines api to see how dynamic tests work */
class PlaygroundEngine : TestEngine {
    override fun getId(): String {
        return "failgood-playground"
    }

    override fun discover(
        discoveryRequest: EngineDiscoveryRequest?,
        uniqueId: UniqueId
    ): TestDescriptor {
        return MyTestDescriptor(TestPlanNode.Container("root"), uniqueId, null)
    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is MyTestDescriptor) return

        val l = request.engineExecutionListener
        l.executionStarted(root)
        val container = TestPlanNode.Container("container")
        val containerDescriptor = DynamicTestDescriptor(container, root)
        l.dynamicTestRegistered(containerDescriptor)
        val container1 = TestPlanNode.Container("container1")
        val container1Descriptor = DynamicTestDescriptor(container1, containerDescriptor)
        l.dynamicTestRegistered(container1Descriptor)

        val container2Descriptor =
            DynamicTestDescriptor(TestPlanNode.Container("container2"), container1Descriptor)
        val test2Descriptor =
            DynamicTestDescriptor(TestPlanNode.Test("Test2"), container2Descriptor)
        container2Descriptor.addChild(test2Descriptor)
        l.executionStarted(containerDescriptor)
        l.executionStarted(container1Descriptor)
        l.dynamicTestRegistered(container2Descriptor)
        l.dynamicTestRegistered(test2Descriptor)
        l.executionStarted(container2Descriptor)
        l.executionStarted(test2Descriptor)
        l.executionFinished(test2Descriptor, TestExecutionResult.successful())
        l.executionFinished(container2Descriptor, TestExecutionResult.successful())

        l.executionStarted(container1Descriptor)
        l.executionFinished(container1Descriptor, TestExecutionResult.successful())
        l.executionFinished(containerDescriptor, TestExecutionResult.successful())
        l.executionFinished(root, TestExecutionResult.successful())
    }
}

class MyTestDescriptor(
    val test: TestPlanNode,
    private val uniqueId: UniqueId,
    private var parent: TestDescriptor?
) : TestDescriptor {
    private val children = mutableSetOf<TestDescriptor>()

    override fun getUniqueId(): UniqueId = uniqueId

    override fun getDisplayName(): String = test.name

    override fun getTags(): MutableSet<TestTag> = mutableSetOf()

    override fun getSource(): Optional<TestSource> = Optional.empty()

    override fun getParent(): Optional<TestDescriptor> = Optional.ofNullable(parent)

    override fun setParent(parent: TestDescriptor?) {
        TODO("Not yet implemented")
    }

    override fun getChildren(): MutableSet<out TestDescriptor> = children

    override fun addChild(descriptor: TestDescriptor) {
        children.add(descriptor)
    }

    override fun removeChild(descriptor: TestDescriptor?) {
        TODO("Not yet implemented")
    }

    override fun removeFromHierarchy() {
        parent = null
    }

    override fun getType(): TestDescriptor.Type {
        return when (test) {
            is TestPlanNode.Container -> TestDescriptor.Type.CONTAINER
            is TestPlanNode.Test -> TestDescriptor.Type.TEST
        }
    }

    override fun findByUniqueId(uniqueId: UniqueId?): Optional<out TestDescriptor> {
        TODO("Not yet implemented")
    }
}
