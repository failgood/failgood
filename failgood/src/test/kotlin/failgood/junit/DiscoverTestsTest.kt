package failgood.junit

import failgood.Test
import failgood.describe
import failgood.internal.StringListTestFilter
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.single

@Test
class DiscoverTestsTest {
    val rootName = ::findContexts.name
    val context = describe(rootName) {
        val testName = "finds a single test with a uniqueId selector"
        it(testName) {
            val className = DiscoverTestsTest::class.qualifiedName!!
            val s = "[engine:failgood]/[class:$rootName($className)]/[method:$testName]"
            println("Uid: $s")
            val request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectUniqueId(UniqueId.parse(s)))
                .build()
            expectThat(findContexts(request)) {
                get { contexts }.single().get { getContexts() }.single().get { this.name }
                    .isEqualTo(rootName)
                get { filter.forClass(className) }.isA<StringListTestFilter>().get { filterList }
                    .containsExactly(rootName, testName)
            }
        }

    }
}
