package failgood.internal

import failgood.TestResult
import failgood.dsl.ResourcesDSL
import failgood.dsl.TestDSL

internal interface ResourcesCloser : ResourcesDSL {
    fun addAfterEach(function: suspend TestDSL.(TestResult) -> Unit)

    fun <T> addClosable(autoCloseable: SuspendAutoCloseable<T>)

    suspend fun closeAutoCloseables()

    suspend fun callAfterEach(testDSL: TestDSL, testResult: TestResult)
}
