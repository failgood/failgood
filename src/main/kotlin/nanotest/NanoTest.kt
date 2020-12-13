package nanotest

class Suite {
    val contexts = mutableListOf<TestContext>()
    fun context(name: String, function: TestContext.() -> Unit) {
        val testContext = TestContext(name)
        contexts.add(testContext)
        testContext.function()
    }

    fun awaitExecution() = SuiteResult(false, contexts.flatMap(TestContext::testFailures), contexts)
}

class TestContext(val name: String) {
    val testFailures = mutableListOf<TestFailure>()

    fun test(testName: String, function: () -> Unit) {
        try {
            function()
        } catch (e: AssertionError) {
            testFailures.add(TestFailure(testName, e))
        }
    }

}

data class SuiteResult(
    val allOk: Boolean,
    val failedTests: Collection<TestFailure>,
    val contexts: MutableList<TestContext>
)

data class TestFailure(val name: String, val throwable: Throwable)
