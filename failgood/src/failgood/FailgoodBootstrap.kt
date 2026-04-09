package failgood

import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.reflect.KClass
import kotlin.system.exitProcess

class FailgoodBootstrap
internal constructor(
    private val testClassFinderFactory: (KClass<*>) -> TestClassFinder,
    private val callerResolver: () -> KClass<*>,
    private val callerClassNameResolver: () -> String,
    private val autotestTimestampPath: Path
) {
    constructor() :
        this(
            testClassFinderFactory = { JvmTestClassFinder.forCaller(it) },
            callerResolver = { findCallerKClass() },
            callerClassNameResolver = ::findCallerName,
            autotestTimestampPath = Paths.get(".failgood.autotest.timestamp"))

    fun findTestClasses(
        classIncludeRegex: Regex = Regex(".*.class\$"),
        newerThan: FileTime? = null,
        randomTestClass: KClass<*> = callerResolver()
    ): MutableList<KClass<*>> {
        return testClassFinderFactory(randomTestClass)
            .findTestClasses(TestClassDiscoveryRequest(classIncludeRegex, newerThan))
    }

    suspend fun autoTest(randomTestClass: KClass<*> = callerResolver()): Unit {
        createAutoTestSuite(randomTestClass)?.run()?.check(false)
    }

    internal fun createAutoTestSuite(randomTestClass: KClass<*> = callerResolver()): Suite? {
        val lastRun =
            try {
                Files.readAttributes(autotestTimestampPath, BasicFileAttributes::class.java)
                    .lastModifiedTime()
            } catch (e: NoSuchFileException) {
                null
            }
        Files.write(autotestTimestampPath, byteArrayOf())
        println("last run:$lastRun")
        val classes = findTestClasses(newerThan = lastRun, randomTestClass = randomTestClass)
        println("will run: ${classes.joinToString { it.simpleName!! }}")
        return if (classes.isNotEmpty()) Suite(classes) else null
    }

    suspend fun runAllTests(
        writeReport: Boolean = false,
        silent: Boolean = false,
        randomTestClass: KClass<*> = callerResolver()
    ): Unit {
        Suite(findTestClasses(randomTestClass = randomTestClass))
            .run(silent = silent)
            .check(writeReport = writeReport)
        printThreads { !it.isDaemon && it.name != "main" }
    }

    fun runTest(): Unit {
        val callerName = callerClassNameResolver().substringBefore("Kt")
        val classloader = callerResolver().java.classLoader
        val suite = Suite(listOf(classloader.loadClass(callerName).kotlin))
        suite.run().check()
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
}
