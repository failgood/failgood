package failgood.junit

import failgood.Test
import failgood.describe
import failgood.internal.StringListTestFilter
import failgood.junit.it.fixtures.packagewith1test.TestFixture
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.engine.discovery.PackageNameFilter
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.single
import java.nio.file.Paths

@Test
class ContextFinderTest {
    private val rootName = "the ContextFinder"
    val context = describe(rootName) {
        val contextFinder = ContextFinder(runTestFixtures = true)
        val testName = "finds a single test with a uniqueId selector"
        it(testName) {
            val className = ContextFinderTest::class.qualifiedName!!
            val s = "[engine:failgood]/[class:$rootName($className)]/[method:$testName]"
            val request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectUniqueId(UniqueId.parse(s)))
                .build()
            expectThat(contextFinder.findContexts(request)) {
                get { contexts }.single().get { getContexts() }.single().get { this.name }
                    .isEqualTo(rootName)
                get { filter.forClass(className) }.isA<StringListTestFilter>().get { filterList }
                    .containsExactly(rootName, testName)
            }
        }
        describe("test filtering") {
            it("supports a classpathRootSelector with a package filter") {
                val path = ContextFinderTest::class.java.protectionDomain.codeSource.location.path
                val request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClasspathRoots(setOf(Paths.get(path))))
                    .configurationParameters(mapOf(FailGoodJunitTestEngineConstants.RUN_TEST_FIXTURES to "true"))
                    .filters(PackageNameFilter.includePackageNames("failgood.junit.it.fixtures.packagewith1test"))
                    .build()
                val contextNames =
                    contextFinder.findContexts(request).contexts.flatMap { it.getContexts() }.map { it.name }
                expectThat(contextNames).containsExactlyInAnyOrder(TestFixture.CONTEXT_NAME)
            }
        }
    }
}
