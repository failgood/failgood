package failgood

import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.reflect.KClass

object FailGood {
    private val bootstrap = FailgoodBootstrap()

    /**
     * finds test classes
     *
     * @param classIncludeRegex regex that included classes must match you can also call
     *   findTestClasses multiple times to run unit tests before integration tests. for
     *   example ```kotlin Suite(findTestClasses(TestClass::class,
     *   Regex(".*Test.class\$)+findTestClasses(TestClass::class, Regex(".*IT.class\$)) ```
     * @param newerThan only return classes that are newer than this. used by autotest
     * @param randomTestClass usually not needed, but you can pass any test class here, and it will
     *   be used to find the classloader and source root
     */
    fun findTestClasses(
        classIncludeRegex: Regex = Regex(".*.class\$"),
        newerThan: FileTime? = null,
        randomTestClass: KClass<*> = findCallerKClass()
    ): MutableList<KClass<*>> =
        bootstrap.findTestClasses(classIncludeRegex, newerThan, randomTestClass)

    internal fun findClassesInPath(
        root: Path,
        classloader: ClassLoader,
        classIncludeRegex: Regex = Regex(".*.class\$"),
        newerThan: FileTime? = null,
        runTestFixtures: Boolean = false,
        matcher: (String) -> Boolean = { true }
    ): MutableList<KClass<*>> =
        JvmTestClassFinder.findClassesInPath(
            root, classloader, classIncludeRegex, newerThan, runTestFixtures, matcher)

    /**
     * runs all changes tests. use with ./gradle -t or run it manually from idea
     *
     * @param randomTestClass usually not needed, but you can pass any test class here, and it will
     *   be used to find the classloader and source root
     */
    @Suppress("RedundantSuspendModifier")
    suspend fun autoTest(randomTestClass: KClass<*> = findCallerKClass()) {
        bootstrap.autoTest(randomTestClass)
    }

    internal fun createAutoTestSuite(randomTestClass: KClass<*> = findCallerKClass()): Suite? =
        bootstrap.createAutoTestSuite(randomTestClass)

    @Suppress("RedundantSuspendModifier")
    suspend fun runAllTests(writeReport: Boolean = false, silent: Boolean = false) {
        bootstrap.runAllTests(writeReport, silent)
    }

    fun runTest(): Unit = bootstrap.runTest()
}

internal fun findCallerKClass(): KClass<*> =
    FailGood::class.java.classLoader.loadClass(findCallerName()).kotlin

internal fun findCallerName(): String = findCallerSTE().className

internal fun findCallerSTE(): StackTraceElement =
    Throwable().stackTrace.first { ste ->
        ste.className.startsWith("failgood.") &&
            ste.fileName?.let {
                !(it == "FailGood.kt" ||
                    it == "FailgoodBootstrap.kt" ||
                    it == "SourceInfo.kt" ||
                    it == "Types.kt" ||
                    it == "Tests.kt" ||
                    it == "Deprecated.kt")
            } ?: true
    }

//    constructor(ste: StackTraceElement) : this(ste.className, ste.fileName!!, ste.lineNumber)

internal actual fun callerSourceInfo(): SourceInfo =
    findCallerSTE().let { SourceInfo(it.className, it.fileName!!, it.lineNumber) }
