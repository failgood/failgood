package failgood.junit

import failgood.TestContainer
import failgood.TestDescription
import failgood.internal.ContextResult
import failgood.internal.FailedContext
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor

internal class FailGoodEngineDescriptor(
    uniqueId: UniqueId,
    val testResult: List<ContextResult>,
    val executionListener: JunitExecutionListener
) :
    EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.displayName) {
    val failedContexts = mutableListOf<FailedContext>()
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
