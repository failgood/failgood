package failgood.junit

import failgood.ContextProvider
import failgood.FailGood
import failgood.FailGoodException
import failgood.ObjectContextProvider
import org.junit.platform.engine.DiscoveryFilter
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.discovery.ClassNameFilter
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.ClasspathRootSelector
import java.nio.file.Paths

suspend fun findContexts(discoveryRequest: EngineDiscoveryRequest): List<ContextProvider> {
    // idea usually sends a classpath selector
    val classPathSelectors = discoveryRequest.getSelectorsByType(ClasspathRootSelector::class.java)

    // gradle sends a class selector for each class
    val classSelectors = discoveryRequest.getSelectorsByType(ClassSelector::class.java)
    val singleClassSelector = discoveryRequest.getSelectorsByType(ClassSelector::class.java).singleOrNull()
    val classNamePredicates =
        discoveryRequest.getFiltersByType(ClassNameFilter::class.java).map { it.toPredicate() }
    return when {
        classPathSelectors.isNotEmpty() -> {
            return classPathSelectors.flatMap { classPathSelector ->
                val uri = classPathSelector.classpathRoot
                FailGood.findClassesInPath(
                    Paths.get(uri),
                    Thread.currentThread().contextClassLoader,
                    matchLambda = { className -> classNamePredicates.all { it.test(className) } }).map {
                    ObjectContextProvider(it)
                }
            }
        }
        classSelectors.isNotEmpty() -> {
            val classes =
                if (classSelectors.size == 1) classSelectors else classSelectors.filter { it.className.endsWith("Test") }
            classes
                .map { ObjectContextProvider(it.javaClass.kotlin) }
        }

        singleClassSelector != null -> {
            listOf(ObjectContextProvider(singleClassSelector.javaClass))
        }
        else -> {
            val message = "unknown selector in discovery request: ${
                discoveryRequestToString(
                    discoveryRequest
                )
            }"
            System.err.println(message)
            throw FailGoodException(
                message
            )
        }
    }
}

private fun discoveryRequestToString(discoveryRequest: EngineDiscoveryRequest): String {
    val allSelectors = discoveryRequest.getSelectorsByType(DiscoverySelector::class.java)
    val allFilters = discoveryRequest.getFiltersByType(DiscoveryFilter::class.java)
    return "selectors:${allSelectors.joinToString()}\nfilters:${allFilters.joinToString()}"
}
