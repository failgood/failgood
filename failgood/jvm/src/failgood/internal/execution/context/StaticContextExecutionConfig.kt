package failgood.internal.execution.context

import failgood.ExecutionListener
import failgood.NullExecutionListener
import failgood.dsl.RootContextLambda
import failgood.internal.ExecuteAllTests
import failgood.internal.TestFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart

internal data class StaticContextExecutionConfig<RootContextDependency>(
    val rootContextLambda: RootContextLambda<RootContextDependency>,
    val scope: CoroutineScope,
    val listener: ExecutionListener = NullExecutionListener,
    val testFilter: TestFilter = ExecuteAllTests,
    val timeoutMillis: Long = 40000,
    val coroutineStart: CoroutineStart = CoroutineStart.DEFAULT,
    val runOnlyTag: String? = null
)
