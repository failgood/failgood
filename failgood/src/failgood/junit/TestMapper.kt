package failgood.junit

import failgood.Context
import failgood.FailGoodException
import failgood.TestDescription
import org.junit.platform.engine.TestDescriptor
import java.util.concurrent.ConcurrentHashMap

internal class TestMapper {
    private val testDescription2JunitTestDescriptor =
        ConcurrentHashMap<TestDescription, TestDescriptor>()
    private val context2JunitTestDescriptor = ConcurrentHashMap<Context, TestDescriptor>()

    fun addMapping(testDescription: TestDescription, testDescriptor: TestDescriptor) {
        testDescription2JunitTestDescriptor[testDescription] = testDescriptor
    }

    fun getMappingOrNull(testDescription: TestDescription) =
        testDescription2JunitTestDescriptor[testDescription]

    fun getMapping(testDescription: TestDescription) =
        getMappingOrNull(testDescription)
            ?: throw FailGoodException("mapping for $testDescription not found")

    fun getMapping(context: Context): TestDescriptor =
        getMappingOrNull(context)
            ?: throw FailGoodException(
                "no mapping found for context $context." +
                    " I have mappings for ${context2JunitTestDescriptor.keys.joinToString()}"
            )

    private fun getMappingOrNull(context: Context) = context2JunitTestDescriptor[context]

    fun addMapping(context: Context, testDescriptor: TestDescriptor) {
        context2JunitTestDescriptor[context] = testDescriptor
    }
}
