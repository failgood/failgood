package failfast

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

class Suite(
    val contexts: Collection<Context>,
    private val parallelism: Int = Runtime.getRuntime().availableProcessors()
) {
    constructor(context: Context, parallelism: Int = Runtime.getRuntime().availableProcessors()) : this(
        listOf(context),
        parallelism
    )

    constructor(parallelism: Int = Runtime.getRuntime().availableProcessors(), function: ContextLambda) : this(
        Context("root", function), parallelism
    )

    init {
        if (contexts.isEmpty()) throw EmptySuiteException()
    }

    fun run(): SuiteResult {
        val testResultChannel = Channel<TestResult>(UNLIMITED)
        val threadPool =
            if (parallelism > 1) Executors.newWorkStealingPool(parallelism) else Executors.newSingleThreadExecutor()
        return try {
            threadPool.asCoroutineDispatcher().use { dispatcher ->
                runBlocking(dispatcher) {
                    val totalTests =
                        contexts.map {
                            async {
                                try {
                                    withTimeout(20000) {
                                        ContextExecutor(it, testResultChannel, this).execute()
                                    }
                                } catch (e: TimeoutCancellationException) {
                                    throw FailFastException("context ${it.name} timed out")
                                }
                            }
                        }.awaitAll()
                            .sum()
                    val results = (0 until totalTests).map {
                        System.out.flush()
                        testResultChannel.receive()
                    }
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

fun describe(subjectDescription: String, function: ContextLambda): Context =
    Context(subjectDescription, function)

interface ContextDSL {
    suspend fun test(name: String, function: TestLambda)

    suspend fun test(ignoredTestName: String)
    suspend fun context(name: String, function: ContextLambda)
    fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T
    suspend fun it(behaviorDescription: String, function: TestLambda)
    suspend fun it(behaviorDescription: String)
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
                val testDescription = """${it.test.parentContexts.joinToString(">")} : ${it.test.testName}"""
                val exceptionInfo = ExceptionPrettyPrinter().prettyPrint(it.failure)

                "$testDescription failed with $exceptionInfo"
            }
            println(message)
            exitProcess(-1)
        }
    }
}

open class FailFastException(override val message: String) : RuntimeException(message)
class SuiteFailedException(private val failedTests: Collection<Failed>) : FailFastException("test failed")

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


data class TestDescriptor(val parentContexts: List<String>, val testName: String)

class ContextExecutor(
    private val context: Context,
    val testResultChannel: Channel<TestResult>,
    val scope: CoroutineScope
) {

    private val finishedContexts = ConcurrentHashMap.newKeySet<List<String>>()!!
    val executedTests = ConcurrentHashMap.newKeySet<TestDescriptor>()!!


    inner class ContextVisitor(private val parentContexts: List<String>, private val resourcesCloser: ResourcesCloser) :
        ContextDSL {
        private var ranATest =
            false // we only run one test per instance so if this is true we don't invoke test lambdas
        var moreTestsLeft = false // are there more tests left to run?

        override suspend fun test(name: String, function: TestLambda) {
            val testDescriptor = TestDescriptor(parentContexts, name)
            if (executedTests.contains(testDescriptor)) {
                return
            } else if (!ranATest) {
                ranATest = true
                executedTests.add(testDescriptor)
                scope.launch {
                    val testResult = try {
                        function()
                        resourcesCloser.close()
                        Success(testDescriptor)
                    } catch (e: AssertionError) {
                        Failed(testDescriptor, e)
                    }
                    testResultChannel.send(testResult)
                }

            } else {
                moreTestsLeft = true
            }
        }

        override suspend fun test(ignoredTestName: String) {
            val testDescriptor = TestDescriptor(parentContexts, ignoredTestName)
            if (executedTests.add(testDescriptor))
                testResultChannel.send(Ignored(testDescriptor))
        }

        override suspend fun context(name: String, function: ContextLambda) {
            if (ranATest) { // if we already ran a test in this context we don't need to visit the child context now
                moreTestsLeft = true // but we need to run the root context again to visit this child context
                return
            }
            val context = parentContexts + name
            if (finishedContexts.contains(context))
                return
            val visitor = ContextVisitor(context, resourcesCloser)
            visitor.function()
            if (visitor.moreTestsLeft)
                moreTestsLeft = true
            else
                finishedContexts.add(context)

            if (visitor.ranATest)
                ranATest = true
        }

        override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T {
            return wrapped.apply { resourcesCloser.add { closeFunction(wrapped) } }
        }

        override suspend fun it(behaviorDescription: String, function: TestLambda) {
            test(behaviorDescription, function)
        }

        override suspend fun it(behaviorDescription: String) {
            test(behaviorDescription)
        }
    }

    suspend fun execute(): Int {
        val function = context.function
        while (true) {
            val resourcesCloser = ResourcesCloser()
            val visitor = ContextVisitor(listOf(context.name), resourcesCloser)
            visitor.function()
            if (!visitor.moreTestsLeft)
                break
        }
        return executedTests.size
    }
}

class ResourcesCloser {
    fun add(autoCloseable: AutoCloseable) {
        closeables.add(autoCloseable)
    }

    fun close() {
        closeables.forEach { it.close() }
    }

    private val closeables = ConcurrentLinkedQueue<AutoCloseable>()

}

class ExceptionPrettyPrinter {
    fun prettyPrint(assertionError: AssertionError): String {
        val stackTrace =
            assertionError.stackTrace.filter { it.lineNumber > 0 }.dropLastWhile { it.fileName != "FailFast.kt" }
        return "${assertionError.message} ${stackTrace.joinToString("\t\n")}"
    }
}

