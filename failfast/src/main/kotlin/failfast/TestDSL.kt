package failfast

@FailFastDSL
interface TestDSL {
    suspend fun println(body: String)
}

class TestDSLImpl(val listener: ExecutionListener, val testDescription: TestDescription) : TestDSL {
    override suspend fun println(body: String) {
        listener.testEvent(testDescription, "stdout", body)
    }
}
