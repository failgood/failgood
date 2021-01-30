package failfast

interface ExecutionListener {
    suspend fun testStarted(testDescriptor: TestDescription)
    suspend fun testFinished(testDescriptor: TestDescription, testResult: TestResult)

}
