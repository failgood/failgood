package nanotest

class Suite {
    val contexts = mutableListOf<TestContext>()
    fun context(name: String, function: TestContext.() -> Unit) {
        val testContext = TestContext(name)
        contexts.add(testContext)
        testContext.function()
    }

    fun run() = SuiteResult(contexts.flatMap(TestContext::testFailures), contexts)
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
    val failedTests: Collection<TestFailure>,
    val contexts: MutableList<TestContext>
) {
    val allOk = failedTests.isEmpty()

    fun check() {
        if (!allOk) throw NanoTestException(failedTests)
    }
}

class NanoTestException(val failedTests: Collection<TestFailure>) : RuntimeException("test failed")

data class TestFailure(val name: String, val throwable: Throwable)
