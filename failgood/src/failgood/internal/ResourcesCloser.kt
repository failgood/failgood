package failgood.internal

import failgood.SuspendAutoCloseable
import failgood.TestResult
import failgood.dsl.ContextOnlyResourceDSL
import failgood.dsl.ResourcesDSL
import failgood.dsl.TestDSL

internal interface ResourcesCloser : ResourcesDSL, ContextOnlyResourceDSL {
    fun addAfterEach(function: suspend TestDSL.(TestResult) -> Unit)

    fun addCloseable(autoCloseable: SuspendAutoCloseable)

    suspend fun closeAutoCloseables()

    suspend fun callAfterEach(testDSL: TestDSL, testResult: TestResult)
}
