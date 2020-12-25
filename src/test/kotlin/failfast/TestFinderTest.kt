package failfast

import strikt.api.expectThat
import strikt.assertions.hasSize
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

fun main() {
    Suite(TestFinderTest.context).run().check()
}

object TestFinderTest {
    val context = describe("test finder") {
        it("can find Test classes") {
            println(measureTimeMillis {
                expectThat(findClasses(TestFinderTest::class)).hasSize(10)
            })
        }
    }
}

fun findClasses(suiteClass: KClass<*>): List<KClass<*>> {
    val classloader = suiteClass.java.classLoader
    val root = Paths.get(suiteClass.java.protectionDomain.codeSource.location.path)
    val results = mutableListOf<KClass<*>>()
    Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
            val path = root.relativize(file!!).toString()
            if (path.endsWith("Test.class")) {
                val kClass = classloader.loadClass(path.substringBefore(".class").replace("/", ".")).kotlin
                results.add(kClass)
            }
            return super.visitFile(file, attrs)
        }

    })
    return results
}


