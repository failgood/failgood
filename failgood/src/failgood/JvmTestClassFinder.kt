package failgood

import failgood.internal.TestFixture
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.reflect.KClass

internal class JvmTestClassFinder(private val root: Path, private val classloader: ClassLoader) :
    TestClassFinder {
    override fun findTestClasses(request: TestClassDiscoveryRequest): MutableList<KClass<*>> =
        findClassesInPath(
            root,
            classloader,
            request.classIncludeRegex,
            request.newerThan,
            request.runTestFixtures,
            request.matcher)

    companion object {
        internal fun forCaller(randomTestClass: KClass<*>): JvmTestClassFinder {
            val classloader = randomTestClass.java.classLoader
            val root = Paths.get(randomTestClass.java.protectionDomain.codeSource.location.toURI())
            return JvmTestClassFinder(root, classloader)
        }

        internal fun findClassesInPath(
            root: Path,
            classloader: ClassLoader,
            classIncludeRegex: Regex = Regex(".*.class\$"),
            newerThan: FileTime? = null,
            runTestFixtures: Boolean = false,
            matcher: (String) -> Boolean = { true }
        ): MutableList<KClass<*>> {
            val results = mutableListOf<KClass<*>>()
            Files.walkFileTree(
                root,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(
                        file: Path?,
                        attrs: BasicFileAttributes?
                    ): FileVisitResult {
                        val path = root.relativize(file!!).toString()
                        if (path.matches(classIncludeRegex) &&
                            (newerThan == null || attrs!!.lastModifiedTime() > newerThan)) {
                            val className =
                                path.substringBefore(".class").replace(File.separatorChar, '.')
                            if (matcher(className)) {
                                val clazz = classloader.loadClass(className)
                                if (clazz.isAnnotationPresent(Test::class.java) ||
                                    (runTestFixtures &&
                                        clazz.isAnnotationPresent(TestFixture::class.java))) {
                                    results.add(clazz.kotlin)
                                }
                            }
                        }
                        return FileVisitResult.CONTINUE
                    }
                })
            return results
        }
    }
}
