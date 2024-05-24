package failgood.junit.it

import failgood.junit.FailGoodJunitTestEngineConstants
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder

fun launcherDiscoveryRequest(
    selectors: List<DiscoverySelector>
): LauncherDiscoveryRequest {
    @Suppress("RemoveExplicitTypeArguments") // necessary because of a compiler bug
    val configurationParameters =
        buildMap<String, String> {
            put(FailGoodJunitTestEngineConstants.CONFIG_KEY_RUN_TEST_FIXTURES, "true")
            put(FailGoodJunitTestEngineConstants.CONFIG_KEY_SILENT, "true")
            put(FailGoodJunitTestEngineConstants.CONFIG_KEY_REPEAT, "1")
        }
    return LauncherDiscoveryRequestBuilder.request()
        .configurationParameters(configurationParameters)
        .selectors(selectors)
        .build()
}
