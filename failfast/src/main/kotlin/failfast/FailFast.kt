package failfast

import failfast.internal.ContextTreeReporter
import failfast.internal.ExceptionPrettyPrinter
import failfast.internal.Junit4Reporter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.reflect.KClass
import kotlin.system.exitProcess

fun runAllTests() {
    Suite.fromClasses(FailFast.findTestClasses()).run().check()
}

data class RootContext(
    val name: String = "root",
    val disabled: Boolean = false,
    val function: ContextLambda
)

typealias ContextLambda = suspend ContextDSL.() -> Unit

typealias TestLambda = suspend () -> Unit

fun context(description: String, disabled: Boolean = false, function: ContextLambda): RootContext =
    RootContext(description, disabled, function)

fun describe(subjectDescription: String, disabled: Boolean = false, function: ContextLambda):
        RootContext = RootContext(subjectDescription, disabled, function)

inline fun <reified T> describe(disabled: Boolean = false, noinline function: ContextLambda):
        RootContext = describe(T::class, disabled, function)

/**
 *
 */
fun describe(subjectType: KClass<*>, disabled: Boolean = false, function: ContextLambda):
        RootContext = RootContext(subjectType.simpleName!!, disabled, function)

interface ContextDSL {
    /**
     * define a test context. A test context contains tests and/or sub contexts
     */
    suspend fun context(name: String, function: ContextLambda)

    /**
     * define a test
     */
    suspend fun test(name: String, function: TestLambda)


    /**
     * define a test context that describes a subject. for use with [ContextDSL.it]
     */
    suspend fun describe(name: String, function: ContextLambda)

    /**
     * create a test dependency that should be closed after the a test run
     * use this instead of beforeEach/afterEach
     */
    fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T

    /**
     * define a test that describes a subject. for use with [ContextDSL.describe]
     */
    suspend fun it(behaviorDescription: String, function: TestLambda)

    /**
     * define a pending test that is not implemented yet.
     */
    fun itWill(behaviorDescription: String, function: TestLambda = {})
}

data class SuiteResult(
    val allTests: List<TestResult>,
    val failedTests: Collection<Failed>,
    val contexts: List<Context>
) {
    private val writer = PrintWriter(FileWriter(File("failfast.log"), true), true)
    val allOk = failedTests.isEmpty()
    private fun println(message: Any) {
        writer.println(message)
        kotlin.io.println(message)
    }

    fun check(throwException: Boolean = false, writeReport: Boolean = false) {

        println(
            ContextTreeReporter(allTests, contexts).stringReport()
                .joinToString("\n")
        )
        //**/build/test-results/test/TEST-*.xml'
        if (writeReport) {
            val reportDir = Paths.get("build", "test-results", "test")
            Files.createDirectories(reportDir)
            Files.write(
                reportDir.resolve("TEST-failfast.xml"),
                Junit4Reporter(allTests).stringReport().joinToString("\n").encodeToByteArray()
            )
        }
        val totalTests = allTests.size
        if (allOk) {
            val slowTests = allTests.filterIsInstance<Success>().sortedBy { -it.timeMicro }.take(5)
            println("Slowest tests:")
            slowTests.forEach { println("${ContextTreeReporter.time(it.timeMicro)}ms ${it.test}") }
            val ignoredTests = allTests.filterIsInstance<Ignored>()
            if (ignoredTests.isNotEmpty()) {
                println("\nIgnored tests:")
                ignoredTests.forEach { println(it.test) }
                val ignored = ignoredTests.size
                println("\n$totalTests tests. ${totalTests - ignored} ok, $ignored ignored. time: ${uptime()}")
                return
            }
            println("\n$totalTests tests. time: ${uptime()}")


            return
        }
        if (throwException) throw SuiteFailedException() else {
            val message =
                failedTests.joinToString(separator = "\n") {
                    val testDescription = it.test.toString()
                    val exceptionInfo = ExceptionPrettyPrinter(it.failure).prettyPrint()

                    "$testDescription failed with $exceptionInfo"
                }
            println("failed tests:\n$message")
            println("$totalTests tests. ${failedTests.size} failed. total time: ${uptime()}")
            exitProcess(-1)
        }
    }
}

data class TestDescriptor(val parentContext: Context, val testName: String) {
    override fun toString(): String {
        return "${parentContext.stringPath()} : $testName"
    }
}

data class Context(val name: String, val parent: Context?) {
    val path: List<String> = parent?.path?.plus(name) ?: listOf(name)
    fun stringPath(): String = path.joinToString(" > ")
}

object FailFast {
    /**
     * finds test classes
     *
     * @param classIncludeRegex regex that included classes must match
     *        you can also call findTestClasses multiple times to run unit tests before integration tests.
     *        for example Suite.fromClasses(findTestClasses(TestClass::class, Regex(".*Test.class\$)+findTestClasses(TestClass::class, Regex(".*IT.class\$))
     *
     * @param newerThan only return classes that are newer than this. used by autotest
     *
     * @param randomTestClass usually not needed but you can pass any test class here,
     *        and it will be used to find the classloader and source root
     */
    fun findTestClasses(
        classIncludeRegex: Regex = Regex(".*Test.class\$"),
        newerThan: FileTime? = null,
        randomTestClass: KClass<*> = findCaller()
    ): MutableList<KClass<*>> {
        val classloader = randomTestClass.java.classLoader
        val root = Paths.get(randomTestClass.java.protectionDomain.codeSource.location.path)
        val results = mutableListOf<KClass<*>>()
        Files.walkFileTree(
            root,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    val path = root.relativize(file!!).toString()
                    if (path.matches(classIncludeRegex) && (newerThan == null || attrs!!.lastModifiedTime() > newerThan)) {
                        results.add(
                            classloader.loadClass(path.substringBefore(".class").replace("/", ".")).kotlin
                        )
                    }
                    return FileVisitResult.CONTINUE
                }
            }
        )
        return results
    }

    fun findCaller() = javaClass.classLoader.loadClass(Throwable().stackTrace.first {
        !(it.fileName?.endsWith("FailFast.kt") ?: true)
    }.className).kotlin
}
