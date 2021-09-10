package failgood

import failgood.internal.Colors.RED
import failgood.internal.Colors.RESET
import failgood.internal.ContextPath
import failgood.internal.ContextTreeReporter
import failgood.internal.ExceptionPrettyPrinter
import failgood.internal.Junit4Reporter
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.reflect.KClass
import kotlin.system.exitProcess


data class RootContext(
    val name: String = "root",
    val disabled: Boolean = false,
    val order: Int = 0,
    val function: ContextLambda
) {
    val stackTraceElement = findCallerSTE()
}

typealias ContextLambda = suspend ContextDSL.() -> Unit

typealias TestLambda = suspend TestDSL.() -> Unit

fun context(description: String, disabled: Boolean = false, order: Int = 0, function: ContextLambda): RootContext =
    RootContext(description, disabled, order, function)

fun describe(subjectDescription: String, disabled: Boolean = false, order: Int = 0, function: ContextLambda):
        RootContext = RootContext(subjectDescription, disabled, order, function)

inline fun <reified T> describe(disabled: Boolean = false, order: Int = 0, noinline function: ContextLambda):
        RootContext = describe(T::class, disabled, order, function)

fun describe(subjectType: KClass<*>, disabled: Boolean = false, order: Int = 0, function: ContextLambda):
        RootContext = RootContext("The ${subjectType.simpleName}", disabled, order, function)

data class SuiteResult(
    val allTests: List<TestPlusResult>,
    val failedTests: List<TestPlusResult>,
    val contexts: List<Context>
) {
    val allOk = failedTests.isEmpty()


    @Suppress("UNREACHABLE_CODE")
    fun check(throwException: Boolean = false, writeReport: Boolean = false) {


        //**/build/test-results/test/TEST-*.xml'
        if (writeReport) {
            val reportDir = Paths.get("build", "test-results", "test")
            Files.createDirectories(reportDir)
            Files.write(
                reportDir.resolve("TEST-failgood.xml"),
                Junit4Reporter(allTests).stringReport().joinToString("\n").encodeToByteArray()
            )
        }
        val totalTests = allTests.size
        if (allOk) {
            if (System.getenv("PRINT_SLOWEST") != null)
                printSlowestTests()
            val pendingTests = allTests.filter { it.isPending }
            if (pendingTests.isNotEmpty()) {
                // printPendingTests(ignoredTests)
                val pending = pendingTests.size
                println(
                    pluralize(totalTests, "test") + ". ${totalTests - pending} ok, $pending pending. time: ${
                        uptime(
                            totalTests
                        )
                    }"
                )
                return
            }
            println(pluralize(totalTests, "test") + ". time: ${uptime(totalTests)}")
            return
        }
        if (throwException) throw SuiteFailedException("test failed") else {

            val message =
                failedTests.joinToString(separator = "\n") {
                    it.prettyPrint()
                }
            @Suppress("unused")
            println("${RED}FAILED:${RESET}\n$message")
            println("$totalTests tests. ${failedTests.size} failed. total time: ${uptime(totalTests)}")
            exitProcess(-1)
        }
        @Suppress("unused")
        fun printPendingTests(pendingTests: List<TestPlusResult>) {
            println("\nPending tests:")
            pendingTests.forEach { println(it.test) }
        }

    }

    private fun printSlowestTests() {
        val contextTreeReporter = ContextTreeReporter()
        val slowTests =
            allTests.filter { it.isSuccess }.sortedBy { 0 - (it.result as Success).timeMicro }.take(5)
        println("Slowest tests:")
        slowTests.forEach { println("${contextTreeReporter.time((it.result as Success).timeMicro)}ms ${it.test}") }
    }

}

data class TestDescription(
    val container: TestContainer,
    val testName: String,
    val stackTraceElement: StackTraceElement
) {
    internal constructor(testPath: ContextPath, stackTraceElement: StackTraceElement) : this(
        testPath.parentContext,
        testPath.name,
        stackTraceElement
    )

    override fun toString(): String {
        return "${container.stringPath()} > $testName"
    }
}

/* something that contains tests */
interface TestContainer {
    val parents: List<TestContainer>
    val name: String
    fun stringPath(): String

}

data class Context(
    override val name: String,
    val parent: Context? = null,
    val stackTraceElement: StackTraceElement? = null
) : TestContainer {
    companion object {
        fun fromPath(path: List<String>): Context {
            return Context(path.last(), if (path.size == 1) null else fromPath(path.dropLast(1)))
        }
    }

    override val parents: List<TestContainer> = parent?.parents?.plus(parent) ?: listOf()
    val path: List<String> = parent?.path?.plus(name) ?: listOf(name)
    override fun stringPath(): String = path.joinToString(" > ")
}

object FailGood {
    /**
     * finds test classes
     *
     * @param classIncludeRegex regex that included classes must match
     *        you can also call findTestClasses multiple times to run unit tests before integration tests.
     *        for example Suite.fromClasses(findTestClasses(TestClass::class, Regex(".*Test.class\$)+findTestClasses(TestClass::class, Regex(".*IT.class\$))
     *
     * @param newerThan only return classes that are newer than this. used by autotest
     *
     * @param randomTestClass usually not needed, but you can pass any test class here,
     *        and it will be used to find the classloader and source root
     */
    suspend fun findTestClasses(
        classIncludeRegex: Regex = Regex(".*Test.class\$"),
        newerThan: FileTime? = null,
        randomTestClass: KClass<*> = findCaller()
    ): MutableList<KClass<*>> {
        val classloader = randomTestClass.java.classLoader
        val root = Paths.get(randomTestClass.java.protectionDomain.codeSource.location.path)
        return findClassesInPath(root, classloader, classIncludeRegex, newerThan)
    }

    @Suppress("RedundantSuspendModifier", "BlockingMethodInNonBlockingContext")
    internal suspend fun findClassesInPath(
        root: Path,
        classloader: ClassLoader,
        classIncludeRegex: Regex = Regex(".*Test.class\$"),
        newerThan: FileTime? = null,
        matchLambda: (String) -> Boolean = { true }
    ): MutableList<KClass<*>> {
        val results = mutableListOf<KClass<*>>()
        Files.walkFileTree(
            root,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    val path = root.relativize(file!!).toString()
                    if (path.matches(classIncludeRegex) && (newerThan == null || attrs!!.lastModifiedTime() > newerThan)) {
                        val className = path.substringBefore(".class").replace("/", ".")
                        if (matchLambda(className))
                            results.add(
                                classloader.loadClass(className).kotlin
                            )
                    }
                    return FileVisitResult.CONTINUE
                }
            }
        )
        return results
    }

    /**
     * runs all changes tests. use with ./gradle -t or run it manually from idea
     *
     * @param randomTestClass usually not needed, but you can pass any test class here,
     *        and it will be used to find the classloader and source root
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun autoTest(randomTestClass: KClass<*> = findCaller()) {
        val timeStampPath = Paths.get(".autotest.failgood")
        val lastRun: FileTime? =
            try {
                Files.readAttributes(timeStampPath, BasicFileAttributes::class.java).lastModifiedTime()
            } catch (e: NoSuchFileException) {
                null
            }
        Files.write(timeStampPath, byteArrayOf())
        println("last run:$lastRun")
        val classes = findTestClasses(newerThan = lastRun, randomTestClass = randomTestClass)
        println("will run: ${classes.joinToString { it.simpleName!! }}")
        if (classes.isNotEmpty()) Suite(classes.map { ObjectContextProvider(it) }).run().check(false)
    }

    suspend fun runAllTests(writeReport: Boolean = false) {
        Suite.fromClasses(findTestClasses()).run().check(writeReport = writeReport)
        printThreads()
    }

    private fun printThreads() {
        val remainingThreads = Thread.getAllStackTraces().filterKeys { !it.isDaemon && it.name != "main" }
        if (remainingThreads.isNotEmpty()) {
            println("Warning: The test suite left some non daemon threads running:")
            remainingThreads
                .forEach { (t, s) -> println("\n* Thread:${t.name}: ${s.joinToString("\n")}") }
            exitProcess(0)
        }
    }

    fun runTest(singleTest: String? = null) {
        val classes = listOf(javaClass.classLoader.loadClass((findCallerName().substringBefore("Kt"))).kotlin)
        val suite = Suite.fromClasses(classes)
        if (singleTest == null)
            suite.run().check()
        else {
            val result = suite.runSingle(singleTest)
            if (result is Failed) {
                println("$singleTest${ExceptionPrettyPrinter(result.failure).prettyPrint()}")
            } else
                println("$singleTest OK")
        }
    }

    // find first class that is not defined in this file.
    private fun findCaller() = javaClass.classLoader.loadClass(findCallerName()).kotlin

}

private fun findCallerName(): String = findCallerSTE().className

private fun findCallerSTE(): StackTraceElement = Throwable().stackTrace.first {
    !(it.fileName?.endsWith("FailGood.kt") ?: true)
}
