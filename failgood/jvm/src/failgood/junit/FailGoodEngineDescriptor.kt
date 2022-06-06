package failgood.junit

import failgood.internal.ContextResult
import failgood.internal.FailedRootContext
import failgood.internal.SuiteExecutionContext
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import java.util.*

internal class FailGoodEngineDescriptor(
    uniqueId: UniqueId,
    val testResult: List<ContextResult>,
    val executionListener: JunitExecutionListener,
    val suiteExecutionContext: SuiteExecutionContext
) : EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.displayName) {
    val failedRootContexts = mutableListOf<FailedRootContext>()
    val mapper = TestMapper()
    fun allDescendants(): String {
        val size = descendants.size
        if (size == 0)
            return "<EMPTY>"
        return descendants.joinToString(postfix = "(total:$size)") { "[UUID:${it.uniqueId} TYPE:${it.type}]" }
    }

    private val failgoodClass: Optional<TestSource> = Optional.of(ClassSource.from(FailGoodJunitTestEngine::class.java))
    override fun getSource(): Optional<TestSource> = failgoodClass
}
