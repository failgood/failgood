package failgood.junit

import failgood.FailGoodException
import failgood.TestContainer
import failgood.TestDescription
import org.junit.platform.engine.TestDescriptor

class TestMapper {
    private val testDescription2JunitTestDescriptor = mutableMapOf<TestDescription, TestDescriptor>()
    private val context2JunitTestDescriptor = mutableMapOf<TestContainer, TestDescriptor>()
    fun addMapping(testDescription: TestDescription, testDescriptor: TestDescriptor) {
        testDescription2JunitTestDescriptor[testDescription] = testDescriptor
    }

    fun getMapping(testDescription: TestDescription): TestDescriptor = testDescription2JunitTestDescriptor[testDescription]
        ?: throw FailGoodException("no mapping found for test $testDescription. I have mappings for ${testDescription2JunitTestDescriptor.keys.joinToString()}")

    fun getMapping(context: TestContainer): TestDescriptor =
        context2JunitTestDescriptor[context] ?: throw FailGoodException("no mapping found for context $context. I have mappings for ${context2JunitTestDescriptor.keys.joinToString()}")

    fun addMapping(context: TestContainer, testDescriptor: TestDescriptor) {
        context2JunitTestDescriptor[context] = testDescriptor
    }
}
