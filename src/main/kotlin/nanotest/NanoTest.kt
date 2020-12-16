package nanotest

class Suite(val contexts: Collection<Context>) {

    init {
        if (contexts.isEmpty()) throw EmptySuiteException()
    }

    constructor(function: ContextLambda) : this(listOf(Context("root", function)))

    fun run(): SuiteResult {

        val results: List<TestResult> = contexts.flatMap { ContextExecutor(it).execute() }

        return SuiteResult(
            results,
            results.filterIsInstance<Failed>()
        )
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


data class SuiteResult(
    val allTests: List<TestResult>,
    val failedTests: Collection<Failed>
) {
    val allOk = failedTests.isEmpty()

    fun check() {
        if (!allOk) throw SuiteFailedException(failedTests)
    }
}

open class NanoTestException(override val message: String) : RuntimeException(message)
class SuiteFailedException(private val failedTests: Collection<Failed>) : NanoTestException("test failed") {
    override fun toString(): String = failedTests.joinToString { it.throwable.stackTraceToString() }
}

sealed class TestResult

data class Success(val test: TestDescriptor) : TestResult()
data class Ignored(val test: TestDescriptor) : TestResult()
class Failed(val name: TestDescriptor, val throwable: Throwable) : TestResult() {
    override fun equals(other: Any?): Boolean {
        return (other is Failed)
                && name == other.name
                && throwable.stackTraceToString() == other.throwable.stackTraceToString()
    }

    override fun hashCode(): Int = name.hashCode() * 31 + throwable.stackTraceToString().hashCode()
}


data class TestDescriptor(val parentContexts: List<String>, val name: String)
class ContextExecutor(private val context: Context) {

    private val testResults = mutableListOf<TestResult>()
    val executedTests = mutableSetOf<TestDescriptor>()

    val contexts = mutableListOf<List<String>>()

    inner class ContextVisitor(private val parentContexts: List<String>) : ContextDSL {
        val closeables = mutableListOf<AutoCloseable>()
        private var ranATest = false
        var moreTestsLeft = false
        override fun test(name: String, function: () -> Unit) {
            val testDescriptor = TestDescriptor(parentContexts, name)
            if (executedTests.contains(testDescriptor)) {
                return
            } else if (!ranATest) {
                executedTests.add(testDescriptor)
                val testResult = try {
                    function()
                    Success(testDescriptor)
                } catch (e: AssertionError) {
                    Failed(testDescriptor, e)
                }
                testResults.add(testResult)
                ranATest = true
            } else {
                moreTestsLeft = true
            }
        }

        override fun xtest(ignoredTestName: String, function: () -> Unit) {
            val testDescriptor = TestDescriptor(parentContexts, ignoredTestName)
            testResults.add(Ignored(testDescriptor))
        }

        override fun context(name: String, function: ContextLambda) {
            // if we already ran a test in this context we don't need to visit the child context now
            if (ranATest) {
                moreTestsLeft = true // but we need to run the root context again to visit this child context
                return
            }
            val context = parentContexts + name
            contexts.add(context)
            val visitor = ContextVisitor(context)
            visitor.function()
            if (visitor.moreTestsLeft)
                moreTestsLeft = true
        }

        override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T {
            closeables.add(AutoCloseable { closeFunction(wrapped) })
            return wrapped
        }
    }

    fun execute(): List<TestResult> {
        val function = context.function
        while (true) {
            val visitor = ContextVisitor(listOf())
            visitor.function()
            visitor.closeables.forEach { it.close() }
            if (!visitor.moreTestsLeft)
                break
        }
        return testResults
    }
}

