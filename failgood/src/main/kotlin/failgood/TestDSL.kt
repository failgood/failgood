package failgood

@FailGoodDSL
interface TestDSL : ResourcesDSL {
    suspend fun println(body: String)

    @Suppress("FunctionName")
    suspend fun _test_event(type: String, body: String)
}
