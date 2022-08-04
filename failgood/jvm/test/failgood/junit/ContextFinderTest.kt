package failgood.junit

import failgood.Test
import failgood.describe
import failgood.internal.StringListTestFilter
import failgood.junit.it.fixtures.packagewith1test.TestFixture
import failgood.problematic.NonFailgoodTest
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.engine.discovery.PackageNameFilter
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
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
            val result = contextFinder.findContexts(request)
            assert(result.contexts.singleOrNull()?.getContexts()?.singleOrNull()?.name == rootName)
            assert(
                result.filter?.forClass(className)?.let {
                    it is StringListTestFilter && it.filterList == listOf(rootName, testName)
                } == true
            )
        }
        it("does not run a class that has no failgood test annotation") {
            val r = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(NonFailgoodTest::class.java))
                .build()
            assert(contextFinder.findContexts(r).contexts.isEmpty())
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