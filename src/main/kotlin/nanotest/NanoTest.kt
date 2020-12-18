package nanotest

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

class Suite(val contexts: Collection<Context>) {
    constructor(function: ContextLambda) : this(listOf(Context("root", function)))

    init {
        if (contexts.isEmpty()) throw EmptySuiteException()
    }

    fun run(): SuiteResult {
        val threadPool = Executors.newFixedThreadPool(16)
        return try {
            threadPool.asCoroutineDispatcher().use { dispatcher ->
                runBlocking(dispatcher) {
                    val results: List<TestResult> =
                        contexts.map { async { ContextExecutor(it).execute() } }.awaitAll().flatten()
                    SuiteResult(results, results.filterIsInstance<Failed>())
                }
            }
        } finally {
            threadPool.shutdown()
        }
    }
}

data class Context(val name: String, val function: ContextLambda)

class EmptySuiteException : NanoTestException("suite can not be empty")

typealias ContextLambda = suspend ContextDSL.() -> Unit

typealias TestLambda = suspend () -> Unit

fun Any.Context(function: ContextLambda): Context =
    Context(this::class.simpleName ?: throw NanoTestException("could not determine object name"), function)

interface ContextDSL {
    suspend fun test(name: String, function: TestLambda)

    @Suppress("UNUSED_PARAMETER", "unused", "SpellCheckingInspection")
    suspend fun xtest(ignoredTestName: String, function: TestLambda)
    suspend fun context(name: String, function: ContextLambda)
    fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T
}


data class SuiteResult(val allTests: List<TestResult>, val failedTests: Collection<Failed>) {
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

    private val finishedContexts = mutableSetOf<List<String>>()
    private val testResults = mutableListOf<TestResult>()
    val executedTests = mutableSetOf<TestDescriptor>()

    val contexts = mutableListOf<List<String>>()

    inner class ContextVisitor(private val parentContexts: List<String>) : ContextDSL {
        val closeables = mutableListOf<AutoCloseable>()
        private var ranATest =
            false // we only run one test per instance so if this is true we don't invoke test lambdas
        var moreTestsLeft = false // are there more tests left to run?

        override suspend fun test(name: String, function: TestLambda) {
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

        override suspend fun xtest(ignoredTestName: String, function: TestLambda) {
            testResults.add(Ignored(TestDescriptor(parentContexts, ignoredTestName)))
        }

        override suspend fun context(name: String, function: ContextLambda) {
            if (ranATest) { // if we already ran a test in this context we don't need to visit the child context now
                moreTestsLeft = true // but we need to run the root context again to visit this child context
                return
            }
            val context = parentContexts + name
            if (finishedContexts.contains(context))
                return
            contexts.add(context)
            val visitor = ContextVisitor(context)
            visitor.function()
            if (visitor.moreTestsLeft)
                moreTestsLeft = true
            else
                finishedContexts.add(context)

            if (visitor.ranATest)
                ranATest = true
        }

        override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T {
            return wrapped.apply { closeables.add(AutoCloseable { closeFunction(wrapped) }) }
        }
    }

    suspend fun execute(): List<TestResult> {
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

