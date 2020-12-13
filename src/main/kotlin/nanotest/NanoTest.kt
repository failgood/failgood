package nanotest

class Suite {
    val contexts = mutableListOf<TestContext>()
    fun context(contextName: String, function: TestContext.() -> Unit) {
        val testContext = TestContext()
        contexts.add(testContext)
        testContext.function()
    }

    fun awaitExecution() = SuiteResult(false, contexts.flatMap(TestContext::testFailures))
}

class TestContext {
    val testFailures = mutableListOf<TestFailure>()

    fun test(testName: String, function: () -> Unit) {
        try {
            function()
        } catch (e: AssertionError) {
            testFailures.add(TestFailure(testName, e))
        }
    }

}

data class SuiteResult(val allOk: Boolean, val failedTests: Collection<TestFailure>)
data class TestFailure(val name: String, val throwable: Throwable)
