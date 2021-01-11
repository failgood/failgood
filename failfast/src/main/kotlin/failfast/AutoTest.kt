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
 * @param randomTestClass usually not needed but you can pass any test class here,
 *        and it will be used to find the classloader and source root
 */
fun autoTest(randomTestClass: KClass<*> = FailFast.findCaller()) {
    val timeStampPath = Paths.get(".autotest.failfast")
    val lastRun: FileTime? =
        try {
            Files.readAttributes(timeStampPath, BasicFileAttributes::class.java).lastModifiedTime()
        } catch (e: NoSuchFileException) {
            null
        }
    Files.write(timeStampPath, byteArrayOf())
    println("last run:$lastRun")
    val classes = FailFast.findTestClasses(newerThan = lastRun, randomTestClass = randomTestClass)
    println("will run: ${classes.joinToString { it.simpleName!! }}")
    if (classes.isNotEmpty()) Suite(classes.map { ObjectContextProvider(it) }).run().check(false)
}
