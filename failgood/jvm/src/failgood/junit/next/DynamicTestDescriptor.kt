package failgood.junit.next

import failgood.junit.appendContext
import failgood.junit.appendTest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.TestTag
import org.junit.platform.engine.UniqueId
import java.util.Optional

class DynamicTestDescriptor(
    private val node: TestPlanNode,
    private val parent: TestDescriptor,
    val path: String = node.name
) :
    TestDescriptor {
        private val p = parent.uniqueId
        private val uniqueId = when (node) {
            is TestPlanNode.Container -> p.appendContext(path)
            is TestPlanNode.Test -> p.appendTest(path)
        }
        private val children = mutableSetOf<TestDescriptor>()
        override fun getUniqueId(): UniqueId = uniqueId

        override fun getDisplayName(): String = node.name

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
            return when (node) {
                is TestPlanNode.Container -> TestDescriptor.Type.CONTAINER
                is TestPlanNode.Test -> TestDescriptor.Type.TEST
            }
        }

        override fun findByUniqueId(uniqueId: UniqueId?): Optional<out TestDescriptor> {
            TODO("Not yet implemented")
        }
    }
