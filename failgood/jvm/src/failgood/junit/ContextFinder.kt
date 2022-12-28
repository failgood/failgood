package failgood.junit

import failgood.ContextProvider
import failgood.FailGood
import failgood.FailGoodException
import failgood.ObjectContextProvider
import failgood.Test
import failgood.internal.ClassTestFilterProvider
import failgood.internal.TestFilterProvider
import failgood.internal.TestFixture
import org.junit.platform.engine.DiscoveryFilter
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.discovery.ClassNameFilter
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.ClasspathRootSelector
import org.junit.platform.engine.discovery.PackageNameFilter
import org.junit.platform.engine.discovery.UniqueIdSelector
import org.junit.platform.launcher.LauncherDiscoveryRequest
import java.nio.file.Paths
import java.util.LinkedList

internal data class ContextsAndFilters(val contexts: List<ContextProvider>, val filter: TestFilterProvider?)
class ContextFinder(private val runTestFixtures: Boolean = false) {
    internal fun findContexts(discoveryRequest: EngineDiscoveryRequest): ContextsAndFilters {
        val filterConfig = mutableMapOf<String, List<String>>()
        val allSelectors = discoveryRequest.getSelectorsByType(DiscoverySelector::class.java)
        val classNamePredicates =
            discoveryRequest.getFiltersByType(ClassNameFilter::class.java).map { it.toPredicate() }
        val packageNamePredicates =
            discoveryRequest.getFiltersByType(PackageNameFilter::class.java).map { it.toPredicate() }
        val allPredicates = classNamePredicates + packageNamePredicates
        val contexts = allSelectors.flatMapTo(LinkedList()) { selector ->
            when (selector) {
                is ClasspathRootSelector -> {
                    val uri = selector.classpathRoot
                    FailGood.findClassesInPath(
                        Paths.get(uri),
                        Thread.currentThread().contextClassLoader,
                        runTestFixtures = runTestFixtures

                    ) { className -> allPredicates.all { it.test(className) } }.map {
                        ObjectContextProvider(it)
                    }
                }
                is ClassSelector -> {
                    if (selector.javaClass.isAnnotationPresent(Test::class.java) ||
                        (runTestFixtures && selector.javaClass.isAnnotationPresent(TestFixture::class.java))
                    )
                        listOf(ObjectContextProvider(selector.javaClass.kotlin))
                    else
                        listOf()
                }
                is UniqueIdSelector -> {
                    val (filterString, className) = parseUniqueIdSelector(selector)
                    filterConfig[className] = filterString
                    val javaClass = try {
                        Thread.currentThread().contextClassLoader.loadClass(className)
                    } catch (e: ClassNotFoundException) {
                        throw FailGoodException("error loading class $className", e)
                    }
                    listOf(ObjectContextProvider(javaClass.kotlin))
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
        return ContextsAndFilters(contexts, if (filterConfig.isEmpty()) null else ClassTestFilterProvider(filterConfig))
    }
}
data class ClassFilter(val filterStringList: List<String>, val className: String)
internal fun parseUniqueIdSelector(selector: UniqueIdSelector): ClassFilter {
    val segments = selector.uniqueId.segments
    val segment1 = segments[1].value
    val rootContextName = segment1.substringBeforeLast("(")
    val filterString = listOf(rootContextName) + segments.drop(2).map { it.value }
    val className = segment1.substringAfterLast("(").substringBefore(")")
    return ClassFilter(filterString, className)
}
internal fun discoveryRequestToString(discoveryRequest: EngineDiscoveryRequest): String {
    val allSelectors = discoveryRequest.getSelectorsByType(DiscoverySelector::class.java)
    val allFilters = discoveryRequest.getFiltersByType(DiscoveryFilter::class.java)
    val allPostDiscoveryFilters = if (discoveryRequest is LauncherDiscoveryRequest)
        discoveryRequest.postDiscoveryFilters.joinToString()
    else "UNKNOWN (${discoveryRequest::class.java.name})"
    return "selectors:${allSelectors.joinToString()}\n" +
        "filters:${allFilters.joinToString()}\n" +
        "postDiscoveryFilters:$allPostDiscoveryFilters"
}
