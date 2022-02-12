package failgood.junit.it

import failgood.junit.FailGoodJunitTestEngine
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder

fun launcherDiscoveryRequest(
    selectors: List<DiscoverySelector>,
    config: Map<String, String> = mapOf()
): LauncherDiscoveryRequest {
    return LauncherDiscoveryRequestBuilder.request()
        .filters(EngineFilter.includeEngines(FailGoodJunitTestEngine().id))
        .configurationParameters(config)
        .selectors(selectors)
        .build()
}
