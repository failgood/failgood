package nanotest

class Suite(val contexts: Collection<Context>) {

    init {
        if (contexts.isEmpty()) throw EmptySuiteException()
    }

    constructor(function: TestContext.() -> Unit) : this(listOf(Context("root", function)))

    fun run(): SuiteResult {
        val results: List<TestContext> = contexts.map { TestContext(it).execute() }
        val allContexts = results.flatMap { it.allChildContexts() } + results
        return SuiteResult(allContexts.flatMap { it.testFailures }, results)
    }
}

class EmptySuiteException : RuntimeException("suite can not be empty")

data class Context(val name: String, val function: TestContext.() -> Unit)

fun Any.Context(function: TestContext.() -> Unit): Context {
    val name = this::class.simpleName ?: throw NanoTestException("could not determine object name")
    return Context(name, function)
}

data class TestContext(val name: String, val function: TestContext.() -> Unit) {
    constructor(context: Context) : this(context.name, context.function)

    private val closables = mutableListOf<AutoCloseable>()
    val testFailures = mutableListOf<TestFailure>()
    private val childContexts = mutableListOf<TestContext>()

    fun allChildContexts(): List<TestContext> = childContexts.flatMap { it.allChildContexts() } + childContexts
    fun test(testName: String, function: () -> Unit) {
        try {
            function()
        } catch (e: AssertionError) {
            testFailures.add(TestFailure(testName, e))
        }
    }

    @Suppress("UNUSED_PARAMETER", "unused")
    fun xtest(ignoredTestName: String, function: () -> Unit) {


    }

    fun context(name: String, function: TestContext.() -> Unit): TestContext {
        val element = TestContext(name, function).execute()
        childContexts.add(element)
        return element
    }

    fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T {
        closables.add(AutoCloseable { closeFunction(wrapped) })
        return wrapped
    }

    fun cleanUp() {
        closables.forEach { it.close() }
    }

    fun execute(): TestContext {
        function()
        cleanUp()
        return this
    }

}

data class SuiteResult(
    val failedTests: Collection<TestFailure>,
    val rootContexts: List<TestContext>
) {
    val allOk = failedTests.isEmpty()

    fun check() {
        if (!allOk) throw SuiteFailedException(failedTests)
    }
}

open class NanoTestException(override val message: String) : RuntimeException(message)
class SuiteFailedException(private val failedTests: Collection<TestFailure>) : NanoTestException("test failed") {
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
