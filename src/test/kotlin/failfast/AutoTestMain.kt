package failfast

import failfast.FailFast.findTestClasses
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.reflect.KClass


fun main() {
    autoTest(ContextExecutorTest::class)
}

fun autoTest(testClass: KClass<*>) {
    val timeStampPath = Paths.get(".autotest.failfast")
    val lastRun: FileTime? =
        try {
            Files.readAttributes(timeStampPath, BasicFileAttributes::class.java).lastModifiedTime()
        } catch (e: NoSuchFileException) {
            null
        }
    Files.writeString(timeStampPath, "")
    println("last run:$lastRun")
    val classes = findTestClasses(testClass, newerThan = lastRun)
    println("will run: ${classes.joinToString { it.simpleName }}")
    if (classes.isNotEmpty())
        Suite(classes.map { ObjectContextProvider(it) }).run().check(false)
}
