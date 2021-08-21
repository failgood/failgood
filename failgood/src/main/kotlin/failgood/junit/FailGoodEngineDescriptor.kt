package failgood.junit

import failgood.TestContainer
import failgood.TestDescription
import failgood.internal.ContextResult
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor

internal class FailGoodEngineDescriptor(
    uniqueId: UniqueId,
    val testResult: List<ContextResult>,
    val executionListener: FailGoodJunitTestEngine.JunitExecutionListener
) :
    EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.displayName) {
    private val testDescription2JunitTestDescriptor = mutableMapOf<TestDescription, TestDescriptor>()
    private val context2JunitTestDescriptor = mutableMapOf<TestContainer, TestDescriptor>()
    fun addMapping(testDescription: TestDescription, testDescriptor: TestDescriptor) {
        testDescription2JunitTestDescriptor[testDescription] = testDescriptor
    }

    fun getMapping(testDescription: TestDescription) = testDescription2JunitTestDescriptor[testDescription]
    fun getMapping(context: TestContainer): TestDescriptor = context2JunitTestDescriptor[context]!!
    fun addMapping(context: TestContainer, testDescriptor: TestDescriptor) {
        context2JunitTestDescriptor[context] = testDescriptor
    }
}
