package failgood.junit

import failgood.FailGoodException
import failgood.TestContainer
import failgood.TestDescription
import java.util.concurrent.ConcurrentHashMap
import org.junit.platform.engine.TestDescriptor

class TestMapper {
    private val testDescription2JunitTestDescriptor =
        ConcurrentHashMap<TestDescription, TestDescriptor>()
    private val context2JunitTestDescriptor = ConcurrentHashMap<TestContainer, TestDescriptor>()

    fun addMapping(testDescription: TestDescription, testDescriptor: TestDescriptor) {
        testDescription2JunitTestDescriptor[testDescription] = testDescriptor
    }

    fun getMappingOrNull(testDescription: TestDescription) =
        testDescription2JunitTestDescriptor[testDescription]

    fun getMapping(testDescription: TestDescription) =
        getMappingOrNull(testDescription)
            ?: throw FailGoodException("mapping for $testDescription not found")

    fun getMapping(context: TestContainer): TestDescriptor =
        getMappingOrNull(context)
            ?: throw FailGoodException(
                "no mapping found for context $context." +
                    " I have mappings for ${context2JunitTestDescriptor.keys.joinToString()}"
            )

    private fun getMappingOrNull(context: TestContainer) = context2JunitTestDescriptor[context]

    fun addMapping(context: TestContainer, testDescriptor: TestDescriptor) {
        context2JunitTestDescriptor[context] = testDescriptor
    }
}
