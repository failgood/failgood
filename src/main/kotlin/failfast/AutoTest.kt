package failfast

import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.reflect.KClass

/**
 * runs all changes tests. use with ./gradle -t or run it manually from idea
 *
 * @param anyTestClass you can pass any test class here, its just used to find the classloader and
 *     source root
 */
fun autoTest(anyTestClass: KClass<*>) {
    val timeStampPath = Paths.get(".autotest.failfast")
    val lastRun: FileTime? =
        try {
            Files.readAttributes(timeStampPath, BasicFileAttributes::class.java).lastModifiedTime()
        } catch (e: NoSuchFileException) {
            null
        }
    Files.write(timeStampPath, byteArrayOf())
    println("last run:$lastRun")
    val classes = FailFast.findTestClasses(anyTestClass, newerThan = lastRun)
    println("will run: ${classes.joinToString { it.simpleName!! }}")
    if (classes.isNotEmpty()) Suite(classes.map { ObjectContextProvider(it) }).run().check(false)
}
