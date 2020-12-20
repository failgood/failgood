package failfast

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Suite(val contexts: Collection<Context>) {
    constructor(function: ContextLambda) : this(listOf(Context("root", function)))

    init {
        if (contexts.isEmpty()) throw EmptySuiteException()
    }

    fun run(): SuiteResult {
        val threadPool = Executors.newWorkStealingPool()
        return try {
            threadPool.asCoroutineDispatcher().use { dispatcher ->
                runBlocking(dispatcher) {
                    val results: List<TestResult> =
                        contexts.map {
                            async {
                                ContextExecutor(it).execute()
                            }
                        }.awaitAll().flatten()
                    SuiteResult(results, results.filterIsInstance<Failed>())
                }
            }
        } finally {
            val threadPoolInfo = threadPool.toString()
            threadPool.awaitTermination(100, TimeUnit.SECONDS)
            threadPool.shutdown()
            println("finished after: ${ManagementFactory.getRuntimeMXBean().uptime}. threadpool: $threadPoolInfo")
        }
    }
}

data class Context(val name: String, val function: ContextLambda)

class EmptySuiteException : FailFastException("suite can not be empty")

typealias ContextLambda = suspend ContextDSL.() -> Unit

typealias TestLambda = suspend () -> Unit

fun Any.context(function: ContextLambda): Context =
    Context(this::class.simpleName ?: throw FailFastException("could not determine object name"), function)

interface ContextDSL {
    suspend fun test(name: String, function: TestLambda)

    @Suppress("UNUSED_PARAMETER", "unused", "SpellCheckingInspection")
    suspend fun xtest(ignoredTestName: String, function: TestLambda)
    suspend fun context(name: String, function: ContextLambda)
    fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T
}


data class SuiteResult(val allTests: List<TestResult>, val failedTests: Collection<Failed>) {
    val allOk = failedTests.isEmpty()

    fun check(throwException: Boolean = true) {
        /*
        allTests.forEach {
            when (it) {
                is Failed -> {
                    println("failed: " + it.test)
                    println(it.throwable.stackTraceToString())
                }
                is Success -> println("success: " + it.test)
                is Ignored -> println("ignored: " + it.test)
            }
        }
         */
        println("${allTests.size} tests")
        if (allOk)
            return
        if (throwException)
            throw SuiteFailedException(failedTests)
        else {
            val message = failedTests.joinToString {
                val testDescription = """${it.test.parentContexts.joinToString(">")} : ${it.test.name}"""
                val exceptionInfo = ExceptionPrettyPrinter().prettyPrint(it.failure)

                "$testDescription failed with $exceptionInfo"
            }
            println(message)
//            exitProcess(-1)
        }
    }
}

open class FailFastException(override val message: String) : RuntimeException(message)
class SuiteFailedException(private val failedTests: Collection<Failed>) : FailFastException("test failed") {
}

sealed class TestResult
data class Success(val test: TestDescriptor) : TestResult()
data class Ignored(val test: TestDescriptor) : TestResult()
class Failed(val test: TestDescriptor, val failure: AssertionError) : TestResult() {
    override fun equals(other: Any?): Boolean {
        return (other is Failed)
                && test == other.test
                && failure.stackTraceToString() == other.failure.stackTraceToString()
    }

    override fun hashCode(): Int = test.hashCode() * 31 + failure.stackTraceToString().hashCode()
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
            val visitor = ContextVisitor(listOf(context.name))
            visitor.function()
            visitor.closeables.forEach { it.close() }
            if (!visitor.moreTestsLeft)
                break
        }
        return testResults
    }
}

class ExceptionPrettyPrinter {
    fun prettyPrint(assertionError: AssertionError): String {
        val stackTrace =
            assertionError.stackTrace.filter { it.lineNumber > 0 }.dropLastWhile { it.fileName != "FailFast.kt" }
        return "${assertionError.message} ${stackTrace.joinToString("\t\n")}"
    }
}

