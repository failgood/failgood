package failgood.junit.it

import failgood.junit.FailGoodJunitTestEngine
import failgood.junit.FailGoodJunitTestEngineConstants
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder

fun launcherDiscoveryRequest(selectors: List<DiscoverySelector>): LauncherDiscoveryRequest {
    return LauncherDiscoveryRequestBuilder.request()
        .filters(EngineFilter.includeEngines(FailGoodJunitTestEngine().id))
        .configurationParameters(
            mapOf(
                FailGoodJunitTestEngineConstants.RUN_TEST_FIXTURES to "true"
//                FailGoodJunitTestEngineConstants.FAILGOOD_NEW_JUNIT to "true"
            )
        )
        .selectors(selectors)
        .build()
}
