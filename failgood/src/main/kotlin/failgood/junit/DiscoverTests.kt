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
import java.util.LinkedList

suspend fun findContexts(discoveryRequest: EngineDiscoveryRequest): List<ContextProvider> {
    val allSelectors = discoveryRequest.getSelectorsByType(DiscoverySelector::class.java)
    val classNamePredicates =
        discoveryRequest.getFiltersByType(ClassNameFilter::class.java).map { it.toPredicate() }


    return allSelectors.flatMapTo(LinkedList()) { selector ->
        when (selector) {
            is ClasspathRootSelector -> {
                val uri = selector.classpathRoot
                FailGood.findClassesInPath(
                    Paths.get(uri),
                    Thread.currentThread().contextClassLoader,
                    matchLambda = { className -> classNamePredicates.all { it.test(className) } }).map {
                    ObjectContextProvider(it)
                }
            }
            is ClassSelector -> {
                listOf(ObjectContextProvider(selector.javaClass.kotlin))
            }
            else -> {
                val message = "unknown selector in discovery request: ${
                    discoveryRequestToString(discoveryRequest)
                }"
                System.err.println(message)
                throw FailGoodException(message)
            }
        }

    }

}

private fun discoveryRequestToString(discoveryRequest: EngineDiscoveryRequest): String {
    val allSelectors = discoveryRequest.getSelectorsByType(DiscoverySelector::class.java)
    val allFilters = discoveryRequest.getFiltersByType(DiscoveryFilter::class.java)
    return "selectors:${allSelectors.joinToString()}\nfilters:${allFilters.joinToString()}"
}
