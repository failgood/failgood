package failgood.internal

import failgood.TestResult
import failgood.dsl.ContextOnlyResourceDSL
import failgood.dsl.ResourcesDSL
import failgood.dsl.TestDSL

internal interface ResourcesCloser : ResourcesDSL, ContextOnlyResourceDSL {
    fun addAfterEach(function: suspend TestDSL.(TestResult) -> Unit)

    fun <T> addClosable(autoCloseable: SuspendAutoCloseable<T>)

    suspend fun closeAutoCloseables()

    suspend fun callAfterEach(testDSL: TestDSL, testResult: TestResult)
}
