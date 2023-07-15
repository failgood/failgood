package failgood.junit.exp

import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.TestTag
import org.junit.platform.engine.UniqueId
import java.util.Optional

class TestNode(val name: String, var _uniqueId: UniqueId? = null, var _parent: TestDescriptor? = null) :
    TestDescriptor {
        override fun getUniqueId(): UniqueId = _uniqueId ?: throw RuntimeException("uniqueId not yet set")

        override fun getDisplayName(): String = name

        override fun getTags(): MutableSet<TestTag> = mutableSetOf()

        override fun getSource(): Optional<TestSource> = Optional.empty()

        override fun getParent(): Optional<TestDescriptor> = Optional.ofNullable(_parent)

        override fun setParent(parent: TestDescriptor?) {
            TODO("Not yet implemented")
        }

        override fun getChildren(): MutableSet<out TestDescriptor> {
            TODO("Not yet implemented")
        }

        override fun addChild(descriptor: TestDescriptor?) {
            TODO("Not yet implemented")
        }

        override fun removeChild(descriptor: TestDescriptor?) {
            TODO("Not yet implemented")
        }

        override fun removeFromHierarchy() {
            TODO("Not yet implemented")
        }

        override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST

        override fun findByUniqueId(uniqueId: UniqueId?): Optional<out TestDescriptor> {
            TODO("Not yet implemented")
        }
    }
