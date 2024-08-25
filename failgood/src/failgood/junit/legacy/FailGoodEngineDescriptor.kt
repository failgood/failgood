package failgood.junit.legacy

import failgood.internal.FailedTestCollectionExecution
import failgood.internal.SuiteExecutionContext
import failgood.internal.TestCollectionExecutionResult
import failgood.junit.FailGoodJunitTestEngineConstants
import failgood.junit.TestMapper
import java.util.*
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.EngineDescriptor

internal class FailGoodEngineDescriptor(
    uniqueId: UniqueId,
    val testResult: List<TestCollectionExecutionResult>,
    val executionListener: ChannelExecutionListener,
    val suiteExecutionContext: SuiteExecutionContext
) : EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.DISPLAY_NAME) {
    val failedRootContexts = mutableListOf<FailedTestCollectionExecution>()
    val mapper = TestMapper()

    fun allDescendants(): String {
        val size = descendants.size
        if (size == 0) return "<EMPTY>"
        return descendants.joinToString(postfix = "(total:$size)") {
            "[UUID:${it.uniqueId} TYPE:${it.type}]"
        }
    }

    private val failgoodClass: Optional<TestSource> =
        Optional.of(ClassSource.from(LegacyJUnitTestEngine::class.java))

    override fun getSource(): Optional<TestSource> = failgoodClass
}
