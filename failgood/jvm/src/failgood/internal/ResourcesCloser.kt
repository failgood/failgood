package failgood.internal

import failgood.ResourcesDSL
import failgood.TestDSL
import failgood.TestResult

internal interface ResourcesCloser : ResourcesDSL {
    fun addAfterEach(function: suspend TestDSL.(TestResult) -> Unit)

    fun <T> addClosable(autoCloseable: SuspendAutoCloseable<T>)

    suspend fun closeAutoCloseables()

    suspend fun callAfterEach(testDSL: TestDSL, testResult: TestResult)
}
