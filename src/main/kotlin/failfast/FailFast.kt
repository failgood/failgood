package failfast

import failfast.internal.ExceptionPrettyPrinter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.system.exitProcess

data class ContextInfo(val context: Context, val tests: Int)


data class RootContext(val name: String, val function: ContextLambda)

class EmptySuiteException : FailFastException("suite can not be empty")

typealias ContextLambda = suspend ContextDSL.() -> Unit

typealias TestLambda = suspend () -> Unit

fun Any.context(function: ContextLambda): RootContext =
    RootContext(this::class.simpleName ?: throw FailFastException("could not determine object name"), function)

fun describe(subjectDescription: String, function: ContextLambda): RootContext =
    RootContext(subjectDescription, function)

fun describe(subjectType: KClass<*>, function: ContextLambda): RootContext =
    RootContext(subjectType.simpleName!!, function)

interface ContextDSL {
    suspend fun test(name: String, function: TestLambda)
    suspend fun test(ignoredTestName: String)
    suspend fun context(name: String, function: ContextLambda)
    fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T
    suspend fun it(behaviorDescription: String, function: TestLambda)
    suspend fun itWill(behaviorDescription: String)
    suspend fun itWill(behaviorDescription: String, function: TestLambda)
}


data class SuiteResult(
    val allTests: List<TestResult>,
    val failedTests: Collection<Failed>,
    val contextInfos: List<ContextInfo>
) {
    val allOk = failedTests.isEmpty()

    fun check(throwException: Boolean = true) {
        /*
        allTests.forEach {
            when (it) {
                is Failed -> {
                    println("failed: " + it.test)
                    println(it.failure.stackTraceToString())
                }
                is Success -> println("success: " + it.test)
                is Ignored -> println("ignored: " + it.test)
            }
        }*/
        println(ContextTreeReporter(allTests, contextInfos.map { it.context }).stringReport().joinToString("\n"))
        println("${allTests.size} tests")
        if (allOk)
            return
        if (throwException)
            throw SuiteFailedException()
        else {
            val message = failedTests.joinToString {
                val testDescription = it.test.toString()
                val exceptionInfo = ExceptionPrettyPrinter().prettyPrint(it.failure)

                "$testDescription failed with $exceptionInfo"
            }
            println(message)
            exitProcess(-1)
        }
    }
}

open class FailFastException(override val message: String) : RuntimeException(message)
class SuiteFailedException : FailFastException("test failed")

sealed class TestResult {
    abstract val test: TestDescriptor
}

data class Success(override val test: TestDescriptor) : TestResult()
data class Ignored(override val test: TestDescriptor) : TestResult()
class Failed(override val test: TestDescriptor, val failure: AssertionError) : TestResult() {
    override fun equals(other: Any?): Boolean {
        return (other is Failed)
                && test == other.test
                && failure.stackTraceToString() == other.failure.stackTraceToString()
    }

    override fun hashCode(): Int = test.hashCode() * 31 + failure.stackTraceToString().hashCode()
}


data class TestDescriptor(val parentContext: Context, val testName: String) {
    override fun toString(): String {
        return """${parentContext.asStringWithPath()} : $testName"""
    }
}

data class Context(val name: String, val parent: Context?) {
    fun asStringWithPath(): String {
        val path = mutableListOf(this)
        var ctx = this
        while (true) {
            ctx = ctx.parent ?: break
            path.add(ctx)
        }
        return path.asReversed().joinToString(" > ") { it.name }
    }
}

internal class ContextExecutor(
    private val rootContext: RootContext,
    val testResultChannel: Channel<TestResult>,
    val scope: CoroutineScope
) {

    private val finishedContexts = ConcurrentHashMap.newKeySet<Context>()!!
    val executedTests = ConcurrentHashMap.newKeySet<TestDescriptor>()!!


    inner class ContextVisitor(private val parentContext: Context, private val resourcesCloser: ResourcesCloser) :
        ContextDSL {
        private var ranATest =
            false // we only run one test per instance so if this is true we don't invoke test lambdas
        var moreTestsLeft = false // are there more tests left to run?

        override suspend fun test(name: String, function: TestLambda) {
            val testDescriptor = TestDescriptor(parentContext, name)
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
            val testDescriptor = TestDescriptor(parentContext, ignoredTestName)
            if (executedTests.add(testDescriptor))
                testResultChannel.send(Ignored(testDescriptor))
        }

        override suspend fun context(name: String, function: ContextLambda) {
            if (ranATest) { // if we already ran a test in this context we don't need to visit the child context now
                moreTestsLeft = true // but we need to run the root context again to visit this child context
                return
            }
            val context = Context(name, parentContext)
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

        override suspend fun itWill(behaviorDescription: String) {
            test(behaviorDescription)
        }

        override suspend fun itWill(behaviorDescription: String, function: TestLambda) {
            test(behaviorDescription)
        }
    }

    suspend fun execute(): Int {
        val function = rootContext.function
        while (true) {
            val resourcesCloser = ResourcesCloser()
            val visitor = ContextVisitor(Context(rootContext.name, null), resourcesCloser)
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


object FailFast {
    private val oldestPossibleFileTime = FileTime.from(0, TimeUnit.SECONDS)!!
    fun findTestClasses(suiteClass: KClass<*>, exclude: String? = null, newerThan: FileTime? = null): List<Class<*>> {
        val compareTo = newerThan ?: oldestPossibleFileTime
        val classloader = suiteClass.java.classLoader
        val root = Paths.get(suiteClass.java.protectionDomain.codeSource.location.path)
        val results = mutableListOf<Class<*>>()
        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                val path = root.relativize(file!!).toString()
                if (path.endsWith("Test.class") && attrs!!.lastModifiedTime() > compareTo
                    && (exclude == null || !path.contains(exclude))
                ) {
                    val jClass = classloader.loadClass(path.substringBefore(".class").replace("/", "."))
                    results.add(jClass)
                }
                return super.visitFile(file, attrs)
            }

        })
        return results
    }
}
