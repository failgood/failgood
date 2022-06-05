package failgood.junit

import failgood.internal.ContextResult
import failgood.internal.FailedRootContext
import failgood.internal.SuiteExecutionContext
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor

internal class FailGoodEngineDescriptor(
    uniqueId: UniqueId,
    val testResult: List<ContextResult>,
    val executionListener: JunitExecutionListener,
    val suiteExecutionContext: SuiteExecutionContext
) : EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.displayName) {
    val failedRootContexts = mutableListOf<FailedRootContext>()
    val mapper = TestMapper()
    fun allDescendants(): String {
        return descendants.joinToString(postfix = "(total:${descendants.size})") { "[UUID:${it.uniqueId} TYPE:${it.type}]" }
    }
}
