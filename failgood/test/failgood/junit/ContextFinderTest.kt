package failgood.junit

import failgood.Test
import failgood.describe
import failgood.internal.StringListTestFilter
import failgood.junit.it.fixtures.packagewith1test.SimpleTestFixture
import failgood.problematic.NonFailgoodTest
import java.nio.file.Paths
import kotlin.test.assertNotNull
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.engine.discovery.PackageNameFilter
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder

@Test
class ContextFinderTest {
    private val rootName = "the ContextFinder"
    val context =
        describe(rootName) {
            val contextFinder = ContextFinder(runTestFixtures = true)
            val testName = "finds a single test with a uniqueId selector"
            it(testName) {
                val className = ContextFinderTest::class.qualifiedName!!
                val s = "[engine:failgood]/[class:$rootName($className)]/[method:$testName]"
                val request =
                    LauncherDiscoveryRequestBuilder.request()
                        .selectors(DiscoverySelectors.selectUniqueId(UniqueId.parse(s)))
                        .build()
                val result = assertNotNull(contextFinder.findContexts(request))
                assert(
                    result.suite.contextProviders
                        .singleOrNull()
                        ?.getContextCreators()
                        ?.singleOrNull()
                        ?.getContexts()
                        ?.singleOrNull()
                        ?.context
                        ?.name == rootName
                )
                assert(
                    result.filter?.forClass(className)?.let {
                        it is StringListTestFilter && it.filterList == listOf(rootName, testName)
                    } == true
                )
            }
            it("does not run a class that has no failgood test annotation") {
                val r =
                    LauncherDiscoveryRequestBuilder.request()
                        .selectors(DiscoverySelectors.selectClass(NonFailgoodTest::class.java))
                        .build()
                assert(contextFinder.findContexts(r) == null)
            }
            describe("test filtering") {
                it("supports a classpathRootSelector with a package filter") {
                    val path =
                        ContextFinderTest::class.java.protectionDomain.codeSource.location.toURI()
                    val request =
                        LauncherDiscoveryRequestBuilder.request()
                            .selectors(
                                DiscoverySelectors.selectClasspathRoots(setOf(Paths.get(path)))
                            )
                            .configurationParameters(
                                mapOf(
                                    FailGoodJunitTestEngineConstants.CONFIG_KEY_RUN_TEST_FIXTURES to
                                        "true"
                                )
                            )
                            .filters(
                                PackageNameFilter.includePackageNames(
                                    "failgood.junit.it.fixtures.packagewith1test"
                                )
                            )
                            .build()
                    val suiteAndFilters = assertNotNull(contextFinder.findContexts(request))
                    val contextNames =
                        suiteAndFilters.suite.contextProviders
                            .flatMap { it.getContextCreators() }
                            .flatMap { it.getContexts() }
                            .map { it.context.name }
                    expectThat(contextNames)
                        .containsExactlyInAnyOrder(SimpleTestFixture.CONTEXT_NAME)
                }
            }
            describe("parsing unique id selectors") {
                it("works when the root contexts contains brackets") {
                    val (className, filterStringList) =
                        DiscoverySelectors.selectUniqueId(
                                UniqueId.parse(
                                    "[engine:failgood]/[class:Root Context (with brackets)(className)]/[method:test name]"
                                )
                            )
                            .toClassFilter()
                    assert(filterStringList == listOf("Root Context (with brackets)", "test name"))
                    assert(className == "className")
                }
            }
        }
}
