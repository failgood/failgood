package failfast

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.system.measureTimeMillis

fun main() {
    Suite(TestFinderTest.context).run().check()
}

object TestFinderTest {
    val context = describe("test finder") {
        it("can find Test classes") {
            println(measureTimeMillis {
                TestFinder(TestFinderTest::class).findClasses()
            })
        }
    }
}

class TestFinder(val kClass: KClass<*>) {
    fun findClasses() {
        val classloader = kClass.java.classLoader
        val root = Paths.get(kClass.java.protectionDomain.codeSource.location.path)
        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                val path = root.relativize(file!!).toString()
                if (path.endsWith("Test.class")) {
                    val kClass = classloader.loadClass(path.substringBefore(".class").replace("/", ".")).kotlin
                    @Suppress("UNUSED_VARIABLE") val contextField =
                        kClass.declaredMemberProperties.single { it.name == "context" }.call(kClass.objectInstance)
                }
                return super.visitFile(file, attrs)
            }

        })
    }


}
