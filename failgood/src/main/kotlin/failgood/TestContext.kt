package failgood

class TestContext(
    resourcesDSL: ResourcesDSL,
    private val listener: ExecutionListener,
    private val testDescription: TestDescription
) : TestDSL, ResourcesDSL by resourcesDSL {
    override suspend fun println(body: String) {
        listener.testEvent(testDescription, "stdout", body)
    }
}
