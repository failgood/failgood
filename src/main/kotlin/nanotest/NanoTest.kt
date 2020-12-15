package nanotest

class Suite(val contexts: Collection<Context>) {

    init {
        if (contexts.isEmpty()) throw EmptySuiteException()
    }

    constructor(function: ContextLambda) : this(listOf(Context("root", function)))

    fun run(): SuiteResult {
        val results: List<TestContext> = contexts.map { TestContext(it).execute() }
        val allContexts = results.flatMap { it.allChildContexts() } + results
        val allTests = allContexts.flatMap { it.testResults }
        return SuiteResult(
            allTests,
            allTests.filterIsInstance<Failed>(),
            allContexts.map { ContextResult(it.name, it.testResults) })
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
    fun test(name: String, function: () -> Unit)

    @Suppress("UNUSED_PARAMETER", "unused")
    fun xtest(ignoredTestName: String, function: () -> Unit)
    fun context(name: String, function: ContextLambda)
    fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T
}

internal data class TestContext(val name: String, val function: ContextLambda) : ContextDSL {
    constructor(context: Context) : this(context.name, context.function)

    private val closeables = mutableListOf<AutoCloseable>()
    val testResults = mutableListOf<TestResult>()
    private val childContexts = mutableListOf<TestContext>()

    fun allChildContexts(): List<TestContext> = childContexts.flatMap { it.allChildContexts() } + childContexts
    override fun test(name: String, function: () -> Unit) {
        try {
            function()
            testResults.add(Success(name))
        } catch (e: AssertionError) {
            testResults.add(Failed(name, e))
        }
    }

    @Suppress("UNUSED_PARAMETER", "unused")
    override fun xtest(ignoredTestName: String, function: () -> Unit) {
    }

    override fun context(name: String, function: ContextLambda) {
        val element = TestContext(name, function).execute()
        childContexts.add(element)
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
    val allTests: List<TestResult>,
    val failedTests: Collection<Failed>,
    val contexts: List<ContextResult>
) {
    val allOk = failedTests.isEmpty()

    fun check() {
        if (!allOk) throw SuiteFailedException(failedTests)
    }
}

data class ContextResult(val name: String, val testResults: List<TestResult>)

open class NanoTestException(override val message: String) : RuntimeException(message)
class SuiteFailedException(private val failedTests: Collection<Failed>) : NanoTestException("test failed") {
    override fun toString(): String = failedTests.joinToString { it.throwable.stackTraceToString() }
}

sealed class TestResult

data class Success(val name: String) : TestResult()

class Failed(val name: String, val throwable: Throwable) : TestResult() {
    override fun equals(other: Any?): Boolean {
        return (other is Failed)
                && name == other.name
                && throwable.stackTraceToString() == other.throwable.stackTraceToString()
    }

    override fun hashCode(): Int = name.hashCode() * 31 + throwable.stackTraceToString().hashCode()
}
