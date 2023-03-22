package failgood

@FailGoodDSL
interface TestDSL : ResourcesDSL {

    /**
     * returns information about the running test. Currently only the name of the test.
     */
    val testInfo: TestInfo

    /**
     * Publish a log entry. This currently only works in the junit platform engine.
     * You can use this for debug output in your test.
     */
    suspend fun log(body: String)

    @Suppress("FunctionName")
    suspend fun _test_event(type: String, body: String)
}

data class TestInfo(val name: String, val context: TestExecutionContext)
