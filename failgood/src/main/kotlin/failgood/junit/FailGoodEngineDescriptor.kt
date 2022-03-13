package failgood.junit

import failgood.internal.ContextResult
import failgood.internal.FailedContext
import failgood.internal.SuiteExecutionContext
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor

internal class FailGoodEngineDescriptor(
    uniqueId: UniqueId,
    val testResult: List<ContextResult>,
    val executionListener: JunitExecutionListener,
    val suiteExecutionContext: SuiteExecutionContext
) : EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.displayName) {
    val failedContexts = mutableListOf<FailedContext>()
    val mapper = TestMapper()
    fun allDescendants(): String {
        return descendants.joinToString { "${it.uniqueId} ${it.type}" }
    }
}
