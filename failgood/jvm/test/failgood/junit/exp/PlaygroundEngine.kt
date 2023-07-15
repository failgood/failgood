package failgood.junit.exp

import failgood.junit.appendContext
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.TestTag
import org.junit.platform.engine.UniqueId
import java.util.Optional

class PlaygroundEngine : TestEngine {
    override fun getId(): String {
        return "failgood-playground"
    }

    override fun discover(discoveryRequest: EngineDiscoveryRequest?, uniqueId: UniqueId): TestDescriptor {
        return MyEngineDescriptor(
            uniqueId,
            setOf(
                TestPlanNode.Container(
                    "container",
                    setOf(TestPlanNode.Container("container1", setOf(TestPlanNode.Test("test1"))))
                )
            )
        ).resolve()
    }

    override fun execute(request: ExecutionRequest?) {
        TODO("Not yet implemented")
    }
}

class ContainerNode(
    val name: String,
    private val children: Set<TestDescriptor>,
    var _parent: TestDescriptor? = null,
    var _uniqueId: UniqueId? = null

) : TestDescriptor {
    override fun getUniqueId(): UniqueId = _uniqueId ?: throw RuntimeException("uniqueid not yet set")

    override fun getDisplayName(): String = name

    override fun getTags(): MutableSet<TestTag> = mutableSetOf()

    override fun getSource(): Optional<TestSource> = Optional.empty()

    override fun getParent(): Optional<TestDescriptor> = Optional.ofNullable(_parent)

    override fun setParent(parent: TestDescriptor?) {
        TODO("Not yet implemented")
    }

    override fun getChildren(): MutableSet<out TestDescriptor> = children.toMutableSet()

    override fun addChild(descriptor: TestDescriptor?) {
        TODO("Not yet implemented")
    }

    override fun removeChild(descriptor: TestDescriptor?) {
        TODO("Not yet implemented")
    }

    override fun removeFromHierarchy() {
        TODO("Not yet implemented")
    }

    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER

    override fun findByUniqueId(uniqueId: UniqueId?): Optional<out TestDescriptor> {
        TODO("Not yet implemented")
    }
}

class MyEngineDescriptor(private val uniqueId: UniqueId, private val children: Set<TestPlanNode>) {

    fun resolve(): TestDescriptor {
        var uniqueId = this.uniqueId
        var parent: TestDescriptor = MyTestDescriptor(TestPlanNode.Container("root", children), uniqueId, null)
        val children = this.children.map {
            MyTestDescriptor(it, uniqueId.appendContext(it.name), parent)
        }
        TODO("Not yet implemented")
    }
}

sealed interface TestPlanNode {
    val name: String

    data class Test(override val name: String) : TestPlanNode
    data class Container(override val name: String, val children: Set<TestPlanNode>) : TestPlanNode
}

class MyTestDescriptor(val test: TestPlanNode, private val uniqueId: UniqueId, val parent: TestDescriptor?) :
    TestDescriptor {
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
            TODO("Not yet implemented")
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
