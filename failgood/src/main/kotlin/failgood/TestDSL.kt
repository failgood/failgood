package failgood

@FailGoodDSL
interface TestDSL : ResourcesDSL {
    val testInfo: TestInfo

    suspend fun println(body: String)

    @Suppress("FunctionName")
    suspend fun _test_event(type: String, body: String)
}

data class TestInfo(val name: String)
