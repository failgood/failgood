package nanotest

class Suite(val contexts: Collection<Context>) {

    init {
        if (contexts.isEmpty()) throw EmptySuiteException()
    }

    constructor(function: TestContext.() -> Unit) : this(listOf(Context("root", function)))

    fun run(): SuiteResult {
        val result = contexts.map { it.execute() }
        return SuiteResult(result.flatMap(TestContext::testFailures), result)
    }
}

class EmptySuiteException : RuntimeException("suite can not be empty") {

}

data class Context(val name: String, private val function: TestContext.() -> Unit) {
    fun execute(): TestContext {
        val testContext = TestContext(name)
        testContext.function()
        return testContext
    }

}

data class TestContext(val name: String) {
    val testFailures = mutableListOf<TestFailure>()

    fun test(testName: String, function: () -> Unit) {
        try {
            function()
        } catch (e: AssertionError) {
            testFailures.add(TestFailure(testName, e))
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun xtest(ignoredTestName: String, function: () -> Unit) {


    }

    fun context(name: String, function: TestContext.() -> Unit) {

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

class NanoTestException(val failedTests: Collection<TestFailure>) : RuntimeException("test failed") {
    override fun toString(): String = failedTests.joinToString { it.throwable.stackTraceToString() }
}

class TestFailure(val name: String, val throwable: Throwable) {
    override fun equals(other: Any?): Boolean {
        return (other is TestFailure)
                && name == other.name
                && throwable.stackTraceToString() == other.throwable.stackTraceToString()
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + throwable.stackTraceToString().hashCode()
        return result
    }
}
