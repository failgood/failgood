package failgood.junit

import failgood.Test
import failgood.describe
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder

@Test
class DiscoverTestsTest {
    val context = describe(::findContexts.name) {
        pending("finds a single test with a uniqueId selector") {
            val request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectUniqueId(UniqueId.parse("[engine:failgood]/[class:root context name]/[method:test]")))
                .build()
            findContexts(request)
        }

    }
}
