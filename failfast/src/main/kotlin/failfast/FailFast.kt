package failfast

import failfast.internal.Colors.RED
import failfast.internal.Colors.RESET
import failfast.internal.ContextTreeReporter
import failfast.internal.Junit4Reporter
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.reflect.KClass
import kotlin.system.exitProcess


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
        RootContext = RootContext("The ${subjectType.simpleName}", disabled, function)

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
    val allOk = failedTests.isEmpty()


    @Suppress("UNREACHABLE_CODE")
    fun check(throwException: Boolean = false, writeReport: Boolean = false) {


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
            // printSlowestTests()
            val ignoredTests = allTests.filterIsInstance<Ignored>()
            if (ignoredTests.isNotEmpty()) {
                // printIgnoredTests(ignoredTests)
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
                    it.prettyPrint()
                }
            @Suppress("unused")
            println("${RED}FAILED:${RESET}\n$message")
            println("$totalTests tests. ${failedTests.size} failed. total time: ${uptime()}")
            exitProcess(-1)
        }
        fun printIgnoredTests(ignoredTests: List<Ignored>) {
            println("\nIgnored tests:")
            ignoredTests.forEach { println(it.test) }
        }

        val contextTreeReporter = ContextTreeReporter()
        fun printSlowestTests() {
            val slowTests = allTests.filterIsInstance<Success>().sortedBy { 0 - it.timeMicro }.take(5)
            println("Slowest tests:")
            slowTests.forEach { println("${contextTreeReporter.time(it.timeMicro)}ms ${it.test}") }
        }
    }

}

data class TestPath(val parentContext: Context, val testName: String) {
    companion object {
        fun fromString(path: String): TestDescription {
            val pathElements = path.split(">").map { it.trim() }
            return TestDescription(Context.fromPath(pathElements.dropLast(1)), pathElements.last())
        }
    }

    override fun toString(): String {
        return "${parentContext.stringPath()} > $testName"
    }
}

data class TestDescription(val parentContext: Context, val testName: String) {
    companion object {
        fun fromString(path: String): TestDescription {
            val pathElements = path.split(">").map { it.trim() }
            return TestDescription(Context.fromPath(pathElements.dropLast(1)), pathElements.last())
        }
    }

    override fun toString(): String {
        return "${parentContext.stringPath()} > $testName"
    }
}

data class Context(val name: String, val parent: Context?, val nodeLocation: NodeLocation? = null) {
    companion object {
        fun fromPath(path: List<String>): Context {
            return Context(path.last(), if (path.size == 1) null else fromPath(path.dropLast(1)))
        }
    }

    val path: List<String> = parent?.path?.plus(name) ?: listOf(name)
    fun stringPath(): String = path.joinToString(" > ")
    override fun toString() = stringPath() + " > " + name
}

data class NodeLocation(val clazz: Class<*>, val line: Int)

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
        return findClassesInPath(root, classloader, classIncludeRegex, newerThan)
    }

    fun findClassesInPath(
        root: Path,
        classloader: ClassLoader,
        classIncludeRegex: Regex = Regex(".*Test.class\$"),
        newerThan: FileTime? = null
    ): MutableList<KClass<*>> {
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

    /**
     * runs all changes tests. use with ./gradle -t or run it manually from idea
     *
     * @param randomTestClass usually not needed but you can pass any test class here,
     *        and it will be used to find the classloader and source root
     */
    fun autoTest(randomTestClass: KClass<*> = findCaller()) {
        val timeStampPath = Paths.get(".autotest.failfast")
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

    fun runAllTests(writeReport: Boolean = false) {
        Suite.fromClasses(findTestClasses()).run().check(writeReport = writeReport)
    }

    fun runTest(singleTest: String? = null) {
        val classes = listOf(javaClass.classLoader.loadClass((findCallerName().substringBefore("Kt"))).kotlin)
        val suite = Suite.fromClasses(classes)
        if (singleTest == null)
            suite.run().check()
        else
            suite.runSingle(singleTest)
    }

    // find first class that is not defined in this file.
    private fun findCaller() = javaClass.classLoader.loadClass(findCallerName()).kotlin

    private fun findCallerName(): String = Throwable().stackTrace.first {
        !(it.fileName?.endsWith("FailFast.kt") ?: true)
    }.className
}
