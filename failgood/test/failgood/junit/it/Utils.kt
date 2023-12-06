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
    @Suppress("RemoveExplicitTypeArguments") // necessary because of a compiler bug
    val configurationParameters =
        buildMap<String, String> {
            put(FailGoodJunitTestEngineConstants.CONFIG_KEY_RUN_TEST_FIXTURES, "true")
            put(FailGoodJunitTestEngineConstants.CONFIG_KEY_SILENT, "true")

            if (newEngine) put(FailGoodJunitTestEngineConstants.CONFIG_KEY_NEW_JUNIT, "true")
            else put(FailGoodJunitTestEngineConstants.CONFIG_KEY_NEW_JUNIT, "false")
        }
    return LauncherDiscoveryRequestBuilder.request()
        .filters(EngineFilter.includeEngines(FailGoodJunitTestEngine().id))
        .configurationParameters(configurationParameters)
        .selectors(selectors)
        .build()
}
