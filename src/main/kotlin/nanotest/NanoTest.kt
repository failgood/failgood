package nanotest

import nanotest.exp.ContextCollector
import nanotest.exp.ContextInfo
import nanotest.exp.TestDescriptor

class Suite(val contexts: Collection<Context>) {

    init {
        if (contexts.isEmpty()) throw EmptySuiteException()
    }

    constructor(function: ContextLambda) : this(listOf(Context("root", function)))

    fun run(): SuiteResult {

        val results: List<ContextInfo> = contexts.map { ContextCollector(it).execute() }
        val allTestResults: List<TestResult> =
            results.flatMap { it.tests.map { test -> TestExecutor(it.rootContext, test).execute() } }

//        val allTests = allContexts.flatMap { it.testResults }
        return SuiteResult(
            allTestResults,
            allTestResults.filterIsInstance<Failed>(), results
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
    val failedTests: Collection<Failed>,
    val contexts: List<ContextInfo>
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

data class Success(val name: String) : TestResult()

class Failed(val name: String, val throwable: Throwable) : TestResult() {
    override fun equals(other: Any?): Boolean {
        return (other is Failed)
                && name == other.name
                && throwable.stackTraceToString() == other.throwable.stackTraceToString()
    }

    override fun hashCode(): Int = name.hashCode() * 31 + throwable.stackTraceToString().hashCode()
}


internal class TestExecutor(private val context: Context, private val test: TestDescriptor) {
    private val closeables = mutableListOf<AutoCloseable>()
    private var testResult: TestResult? = null
    fun execute(): TestResult {
        val dsl: ContextDSL = contextDSL(test.parentContexts)
        dsl.(context.function)()
        closeables.forEach { it.close() }
        return testResult!!
    }

    inner class ContextFinder(private val contexts: List<String>) : ContextDSL {
        override fun test(name: String, function: () -> Unit) {
        }

        override fun xtest(ignoredTestName: String, function: () -> Unit) {
        }

        override fun context(name: String, function: ContextLambda) {
            if (contexts.first() != name)
                return

            contextDSL(contexts.drop(1)).function()
        }

        override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T {
            closeables.add(AutoCloseable { closeFunction(wrapped) })
            return wrapped
        }

    }

    private fun contextDSL(parentContexts: List<String>) = if (parentContexts.isEmpty())
        TestFinder(test.name)
    else
        ContextFinder(parentContexts)

    inner class TestFinder(private val name: String) : ContextDSL {
        override fun test(name: String, function: () -> Unit) {
            if (this.name == name)
                testResult = try {
                    function()
                    Success(name)
                } catch (e: AssertionError) {
                    Failed(name, e)
                }
        }

        override fun xtest(ignoredTestName: String, function: () -> Unit) {
        }

        override fun context(name: String, function: ContextLambda) {
        }

        override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T {
            closeables.add(AutoCloseable { closeFunction(wrapped) })
            return wrapped
        }

    }
}
