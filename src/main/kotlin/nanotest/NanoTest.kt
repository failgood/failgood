package nanotest

class Suite(val contexts: Collection<Context>) {

    init {
        if (contexts.isEmpty()) throw EmptySuiteException()
    }

    constructor(function: ContextLambda) : this(listOf(Context("root", function)))

    fun run(): SuiteResult {
        val results: List<TestContext> = contexts.map { TestContext(it).execute() }
        val allContexts = results.flatMap { it.allChildContexts() } + results
        return SuiteResult(allContexts.flatMap { it.testFailures }, allContexts)
    }
}

class EmptySuiteException : RuntimeException("suite can not be empty")

data class Context(val name: String, val function: ContextLambda)

typealias ContextLambda = ContextDSL.() -> Unit

fun Any.Context(function: ContextLambda): Context {
    val name = this::class.simpleName ?: throw NanoTestException("could not determine object name")
    return Context(name, function)
}

interface ContextDSL {
    fun test(testName: String, function: () -> Unit)

    @Suppress("UNUSED_PARAMETER", "unused")
    fun xtest(ignoredTestName: String, function: () -> Unit)
    fun context(name: String, function: ContextLambda): TestContext
    fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T
}

data class TestContext(val name: String, val function: ContextLambda) : ContextDSL {
    constructor(context: Context) : this(context.name, context.function)

    private val closeables = mutableListOf<AutoCloseable>()
    val testFailures = mutableListOf<TestFailure>()
    private val childContexts = mutableListOf<TestContext>()

    fun allChildContexts(): List<TestContext> = childContexts.flatMap { it.allChildContexts() } + childContexts
    override fun test(testName: String, function: () -> Unit) {
        try {
            function()
        } catch (e: AssertionError) {
            testFailures.add(TestFailure(testName, e))
        }
    }

    @Suppress("UNUSED_PARAMETER", "unused")
    override fun xtest(ignoredTestName: String, function: () -> Unit) {
    }

    override fun context(name: String, function: ContextLambda): TestContext {
        val element = TestContext(name, function).execute()
        childContexts.add(element)
        return element
    }

    override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T {
        closeables.add(AutoCloseable { closeFunction(wrapped) })
        return wrapped
    }

    fun execute(): TestContext {
        function()
        closeables.forEach { it.close() }
        return this
    }
}

data class SuiteResult(
    val failedTests: Collection<TestFailure>,
    val contexts: List<TestContext>
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

    override fun hashCode(): Int = name.hashCode() * 31 + throwable.stackTraceToString().hashCode()
}
