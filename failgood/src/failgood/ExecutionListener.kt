package failgood

interface ExecutionListener {
    suspend fun testStarted(testDescription: TestDescription) {}

    /**
     * this is called when a test is finished or skipped. for skipped tests we don't call
     * testStarted
     */
    suspend fun testFinished(testPlusResult: TestPlusResult) {}

    suspend fun testEvent(testDescription: TestDescription, type: String, payload: String) {}

    suspend fun testDiscovered(testDescription: TestDescription) {}

    suspend fun contextDiscovered(context: Context) {}
}
