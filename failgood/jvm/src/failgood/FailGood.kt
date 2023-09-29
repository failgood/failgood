package failgood

import failgood.internal.TestFixture
import failgood.internal.sysinfo.cpus
import java.io.File
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

object FailGood {
    /**
     * finds test classes
     *
     * @param classIncludeRegex regex that included classes must match
     *        you can also call findTestClasses multiple times to run unit tests before integration tests.
     *        for example
     *        ```kotlin
     *        Suite(findTestClasses(TestClass::class, Regex(".*Test.class\$)+findTestClasses(TestClass::class, Regex(".*IT.class\$))
     *        ```
     *
     * @param newerThan only return classes that are newer than this. used by autotest
     *
     * @param randomTestClass usually not needed, but you can pass any test class here,
     *        and it will be used to find the classloader and source root
     */
    fun findTestClasses(
        classIncludeRegex: Regex = Regex(".*.class\$"),
        newerThan: FileTime? = null,
        randomTestClass: KClass<*> = findCaller()
    ): MutableList<KClass<*>> {
        val classloader = randomTestClass.java.classLoader
        val root = Paths.get(randomTestClass.java.protectionDomain.codeSource.location.toURI())
        return findClassesInPath(root, classloader, classIncludeRegex, newerThan)
    }

    internal fun findClassesInPath(
        root: Path,
        classloader: ClassLoader,
        classIncludeRegex: Regex = Regex(".*.class\$"),
        newerThan: FileTime? = null,
        runTestFixtures: Boolean = false,
        matchLambda: (String) -> Boolean = { true }
    ): MutableList<KClass<*>> {
        val results = mutableListOf<KClass<*>>()
        Files.walkFileTree(
            root,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    val path = root.relativize(file!!).toString()
                    if (path.matches(classIncludeRegex) &&
                        (newerThan == null || attrs!!.lastModifiedTime() > newerThan)
                    ) {
                        val className = path.substringBefore(".class").replace(File.separatorChar, '.')
                        if (matchLambda(className)) {
                            val clazz = classloader.loadClass(className)
                            if (clazz.isAnnotationPresent(Test::class.java) ||
                                (runTestFixtures && clazz.isAnnotationPresent(TestFixture::class.java))
                            ) results.add(clazz.kotlin)
                        }
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
    @Suppress("RedundantSuspendModifier")
    suspend fun autoTest(randomTestClass: KClass<*> = findCaller()) {
        createAutoTestSuite(randomTestClass)?.run()?.check(false)
    }

    internal fun createAutoTestSuite(randomTestClass: KClass<*> = findCaller()): Suite? {
        val timeStampPath = Paths.get(".failgood.autotest.timestamp")
        val lastRun: FileTime? = try {
            Files.readAttributes(timeStampPath, BasicFileAttributes::class.java).lastModifiedTime()
        } catch (e: NoSuchFileException) {
            null
        }
        Files.write(timeStampPath, byteArrayOf())
        println("last run:$lastRun")
        val classes = findTestClasses(newerThan = lastRun, randomTestClass = randomTestClass)
        println("will run: ${classes.joinToString { it.simpleName!! }}")
        return if (classes.isNotEmpty()) Suite(classes.map { ObjectContextProvider(it) })
        else null
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun runAllTests(writeReport: Boolean = false, paralellism: Int = cpus()) {
        Suite(findTestClasses()).run(parallelism = paralellism).check(writeReport = writeReport)
        printThreads { !it.isDaemon && it.name != "main" }
    }

    private fun printThreads(filter: (Thread) -> Boolean) {
        val remainingThreads = Thread.getAllStackTraces().filterKeys(filter)
        if (remainingThreads.isNotEmpty()) {
            remainingThreads.forEach { (thread, stackTraceElements) ->
                println("\n* Thread:${thread.name}: ${stackTraceElements.joinToString("\n")}")
            }
            exitProcess(0)
        }
    }

    fun runTest() {
        val classes = listOf(javaClass.classLoader.loadClass((findCallerName().substringBefore("Kt"))).kotlin)
        val suite = Suite(classes)
        suite.run().check()
    }

    // find first class that is not defined in this file.
    private fun findCaller() = javaClass.classLoader.loadClass(findCallerName()).kotlin
}

private fun findCallerName(): String = findCallerSTE().className

internal fun findCallerSTE(): StackTraceElement = Throwable().stackTrace.first { ste ->
    ste.fileName?.let {
        !(it.endsWith("FailGood.kt") || it.endsWith("SourceInfo.kt") || it.endsWith("Types.kt"))
    } ?: true
}
//    constructor(ste: StackTraceElement) : this(ste.className, ste.fileName!!, ste.lineNumber)

internal fun callerSourceInfo(): SourceInfo =
    findCallerSTE().let { SourceInfo(it.className, it.fileName!!, it.lineNumber) }
