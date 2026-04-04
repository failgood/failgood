package failgood

import java.nio.file.attribute.FileTime
import kotlin.reflect.KClass

class TestClassDiscoveryRequest(
    val classIncludeRegex: Regex = Regex(".*.class\$"),
    val newerThan: FileTime? = null,
    val runTestFixtures: Boolean = false,
    val matcher: (String) -> Boolean = { true }
)

fun interface TestClassFinder {
    fun findTestClasses(request: TestClassDiscoveryRequest): MutableList<KClass<*>>
}
