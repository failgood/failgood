package failfast

interface ExecutionListener {
    suspend fun testStarted(testDescriptor: TestDescription)
    suspend fun testFinished(testDescriptor: TestDescription, testResult: TestResult)
    suspend fun testFinished(testPlusResult: TestPlusResult) {
        testFinished(testPlusResult.test, testPlusResult.result)
    }
}
