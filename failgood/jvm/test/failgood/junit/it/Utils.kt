package failgood.junit.it

import failgood.junit.FailGoodJunitTestEngine
import failgood.junit.FailGoodJunitTestEngineConstants
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder

fun launcherDiscoveryRequest(
    selectors: List<DiscoverySelector>,
    newEngine: Boolean = false
): LauncherDiscoveryRequest {
    val configurationParameters =
        buildMap<String, String> {
            put(FailGoodJunitTestEngineConstants.RUN_TEST_FIXTURES, "true")
            if (newEngine) put(FailGoodJunitTestEngineConstants.FAILGOOD_NEW_JUNIT, "true")
            else put(FailGoodJunitTestEngineConstants.FAILGOOD_NEW_JUNIT, "false")
        }
    return LauncherDiscoveryRequestBuilder.request()
        .filters(EngineFilter.includeEngines(FailGoodJunitTestEngine().id))
        .configurationParameters(configurationParameters)
        .selectors(selectors)
        .build()
}
