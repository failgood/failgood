package failfast

class TestContext(private val listener: ExecutionListener, private val testDescription: TestDescription) : TestDSL {
    override suspend fun println(body: String) {
        listener.testEvent(testDescription, "stdout", body)
    }
}
