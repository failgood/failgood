package failgood.junit

import failgood.Test
import failgood.describe
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.single

@Test
class DiscoverTestsTest {
    val rootName = ::findContexts.name
    val context = describe(rootName) {
        it("finds a single test with a uniqueId selector") {
            val className = DiscoverTestsTest::class.qualifiedName
            val request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectUniqueId(UniqueId.parse("[engine:failgood]/[class:$rootName($className)]/[method:finds a single test with a uniqueId selector]")))
                .build()
            expectThat(findContexts(request)).single().get { getContexts() }.single().get { this.name }
                .isEqualTo(rootName)
        }

    }
}
