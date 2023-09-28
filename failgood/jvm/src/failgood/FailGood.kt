package failgood

import failgood.internal.ContextPath
import failgood.internal.TestFixture
import failgood.internal.util.niceString
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.system.exitProcess

fun describe(
    subjectDescription: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextLambda
): RootContext = RootContext(subjectDescription, ignored, order, isolation, function = function)

inline fun <reified T> describe(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    noinline function: ContextLambda
): RootContext = describe(typeOf<T>(), ignored, order, isolation, function)

fun describe(
    subjectType: KType,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextLambda
): RootContext = RootContext(subjectType.niceString(), ignored, order, isolation, function = function)

fun describe(
    subjectType: KClass<*>,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextLambda
): RootContext = RootContext("${subjectType.simpleName}", ignored, order, isolation, function = function)

suspend inline fun <reified Class> ContextDSL<*>.describe(
    tags: Set<String> = setOf(),
    isolation: Boolean? = null,
    ignored: Ignored? = null,
    noinline contextLambda: ContextLambda
) = this.describe(Class::class.simpleName!!, tags, isolation, ignored, contextLambda)

data class TestDescription(
    val container: Context,
    val testName: String,
    val sourceInfo: SourceInfo
) {
    internal constructor(testPath: ContextPath, sourceInfo: SourceInfo) : this(
        testPath.container, testPath.name, sourceInfo
    )

    override fun toString(): String {
        return "${container.stringPath()} > $testName"
    }
}

internal sealed interface LoadResult {
    val order: Int
}

internal data class CouldNotLoadContext(val reason: Throwable, val kClass: KClass<out Any>) : LoadResult {
    override val order: Int
        get() = 0
}

fun RootContext(
    name: String = "root",
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    sourceInfo: SourceInfo = SourceInfo(findCallerSTE()),
    function: ContextLambda
) = RootContext(Context(name, null, sourceInfo, isolation), order, ignored, function)

data class RootContext(
    val context: Context,
    override val order: Int = 0,
    val ignored: Ignored?,
    val function: ContextLambda
) : LoadResult, failgood.internal.Path {
    val sourceInfo: SourceInfo
        get() = context.sourceInfo!! // in the root context we are sure that we always have a sourceInfo
    override val path: List<String>
        get() = listOf(context.name)
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
    val sourceInfo: SourceInfo? = null,
    val isolation: Boolean = true
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

    fun createAutoTestSuite(randomTestClass: KClass<*> = findCaller()): Suite? {
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
    ste.fileName?.let { !(it.endsWith("FailGood.kt") || it.endsWith("SourceInfo.kt")) } ?: true
}
