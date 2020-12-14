package nanotest

class Suite(val contexts: Collection<Context>) {
    constructor(function: TestContext.() -> Unit) : this(listOf(Context("root", function)))

    fun run(): SuiteResult {
        val result = contexts.map { it.execute() }
        return SuiteResult(result.flatMap(TestContext::testFailures), result)
    }
}

class Context(val name: String, private val function: TestContext.() -> Unit) {
    fun execute(): TestContext {
        val testContext = TestContext(name)
        testContext.function()
        return testContext
    }

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
    val contexts: List<TestContext>
) {
    val allOk = failedTests.isEmpty()

    fun check() {
        if (!allOk) throw NanoTestException(failedTests)
    }
}

class NanoTestException(val failedTests: Collection<TestFailure>) : RuntimeException("test failed")

data class TestFailure(val name: String, val throwable: Throwable)
