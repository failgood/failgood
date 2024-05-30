package failgood.junit

import org.junit.platform.engine.DiscoveryFilter
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.launcher.LauncherDiscoveryRequest

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
