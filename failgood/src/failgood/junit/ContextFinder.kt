package failgood.junit

import failgood.FailGood
import failgood.FailGoodException
import failgood.ObjectContextProvider
import failgood.Suite
import failgood.Test
import failgood.internal.ClassTestFilterProvider
import failgood.internal.TestFilterProvider
import failgood.internal.TestFixture
import java.nio.file.Paths
import java.util.LinkedList
import org.junit.platform.engine.DiscoveryFilter
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.discovery.ClassNameFilter
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.ClasspathRootSelector
import org.junit.platform.engine.discovery.MethodSelector
import org.junit.platform.engine.discovery.PackageNameFilter
import org.junit.platform.engine.discovery.UniqueIdSelector
import org.junit.platform.launcher.LauncherDiscoveryRequest

internal data class SuiteAndFilters(
    val suite: Suite,

    // this is currently only used for running single tests by unique id.
    val filter: TestFilterProvider?
)

class ContextFinder(private val runTestFixtures: Boolean = false) {
    internal fun findContexts(discoveryRequest: EngineDiscoveryRequest): SuiteAndFilters? {
        val filterConfig = mutableMapOf<String, List<String>>()
        val allSelectors = discoveryRequest.getSelectorsByType(DiscoverySelector::class.java)
        val classNamePredicates =
            discoveryRequest.getFiltersByType(ClassNameFilter::class.java).map { it.toPredicate() }
        val packageNamePredicates =
            discoveryRequest.getFiltersByType(PackageNameFilter::class.java).map {
                it.toPredicate()
            }
        val allPredicates = classNamePredicates + packageNamePredicates
        val contexts =
            allSelectors.flatMapTo(LinkedList()) { selector ->
                when (selector) {
                    is ClasspathRootSelector -> {
                        val uri = selector.classpathRoot
                        FailGood.findClassesInPath(
                                Paths.get(uri),
                                Thread.currentThread().contextClassLoader,
                                runTestFixtures = runTestFixtures
                            ) { className ->
                                allPredicates.all { it.test(className) }
                            }
                            .map { ObjectContextProvider(it) }
                    }
                    is ClassSelector -> {
                        if (
                            selector.javaClass.isAnnotationPresent(Test::class.java) ||
                                (runTestFixtures &&
                                    selector.javaClass.isAnnotationPresent(TestFixture::class.java))
                        )
                            listOf(ObjectContextProvider(selector.javaClass.kotlin))
                        else listOf()
                    }
                    is UniqueIdSelector -> {
                        val (className, filterString) = selector.toClassFilter()
                        filterConfig[className] = filterString
                        val classFromUniqueId =
                            try {
                                Thread.currentThread().contextClassLoader.loadClass(className)
                            } catch (e: ClassNotFoundException) {
                                throw FailGoodException(
                                    "could not load class for uniqueId $selector",
                                    e
                                )
                            }
                        listOf(ObjectContextProvider(classFromUniqueId.kotlin))
                    }
                    is MethodSelector -> {
                        val result =
                            selector.javaMethod.invoke(
                                ObjectContextProvider.instantiateClassOrObject(selector.javaClass)
                            )
                        if (result is Suite) {
                            return SuiteAndFilters(result, null)
                        }
                        listOf()
                    }
                    else -> {
                        val message =
                            "unknown selector in discovery request: ${
                                discoveryRequest.niceString()
                    }"
                        System.err.println(message)
                        throw FailGoodException(message)
                    }
                }
            }
        return if (contexts.isEmpty()) null
        else
            SuiteAndFilters(
                Suite(contexts),
                if (filterConfig.isEmpty()) null else ClassTestFilterProvider(filterConfig)
            )
    }
}

data class ClassFilter(val className: String, val filterStringList: List<String>)

internal fun UniqueIdSelector.toClassFilter(): ClassFilter {
    val segments = uniqueId.segments
    val segment1 = segments[1].value
    val rootContextName = segment1.substringBeforeLast("(")
    val filterString = listOf(rootContextName) + segments.drop(2).map { it.value }
    val className = segment1.substringAfterLast("(").substringBefore(")")
    return ClassFilter(className, filterString)
}

internal fun EngineDiscoveryRequest.niceString(): String {
    val allSelectors = getSelectorsByType(DiscoverySelector::class.java)
    val allFilters = getFiltersByType(DiscoveryFilter::class.java)
    val allPostDiscoveryFilters =
        if (this is LauncherDiscoveryRequest) postDiscoveryFilters.joinToString()
        else "UNKNOWN (${this::class.java.name})"
    return "selectors:${allSelectors.joinToString()}\n" +
        "filters:${allFilters.joinToString()}\n" +
        "postDiscoveryFilters:$allPostDiscoveryFilters"
}
