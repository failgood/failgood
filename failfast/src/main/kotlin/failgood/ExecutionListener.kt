package failgood

interface ExecutionListener {
    suspend fun testStarted(testDescription: TestDescription)
    suspend fun testFinished(testPlusResult: TestPlusResult)
    suspend fun testEvent(testDescription: TestDescription, type: String, payload: String)
}
