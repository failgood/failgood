package failgood.junit.it

import failgood.Test
import failgood.describe
import failgood.junit.FailGoodJunitTestEngine
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_TEST_CLASS_SUFFIX
import failgood.junit.it.fixtures.DuplicateRootWithOneTest
import failgood.junit.it.fixtures.DuplicateTestNameTest
import failgood.junit.it.fixtures.FailingContext
import failgood.junit.it.fixtures.FailingRootContext
import failgood.junit.it.fixtures.PendingTestFixtureTest
import failgood.junit.it.fixtures.TestFixtureTest
import failgood.junit.it.fixtures.TestWithNestedContextsTest
import kotlinx.coroutines.CompletableDeferred
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

fun launcherDiscoveryRequest(
    selectors: List<DiscoverySelector>,
    config: Map<String, String> = mapOf()
): LauncherDiscoveryRequest {
    return LauncherDiscoveryRequestBuilder.request()
        .filters(EngineFilter.includeEngines(FailGoodJunitTestEngine().id))
        .configurationParameters(config)
        .selectors(selectors)
        .build()
}

@Test
class JunitPlatformFunctionalTest {
    @Suppress("unused")
    val context = describe("The Junit Platform Engine") {
        it("can execute test in a class") {
            val listener = TEListener()
            LauncherFactory.create().execute(
                launcherDiscoveryRequest(
                    listOf(
                        selectClass(
                            DuplicateTestNameTest::class.qualifiedName
                        )
                    )
                ), listener
            )
            expectThat(listener.rootResult.await()).get { status }.isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
        }
        it("works for a failing context or root context") {
            val listener = TEListener()
            val selectors =
                listOf(
                    DuplicateRootWithOneTest::class,
                    DuplicateTestNameTest::class,
                    FailingContext::class,
                    FailingRootContext::class,
                    PendingTestFixtureTest::class,
                    TestFixtureTest::class,
                    TestWithNestedContextsTest::class
                ).map { selectClass(it.qualifiedName) }
            LauncherFactory.create().execute(
                launcherDiscoveryRequest(selectors, mapOf(CONFIG_KEY_TEST_CLASS_SUFFIX to "")), listener
            )
            expectThat(listener.rootResult.await()) {
                get { status }.isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
            }
            expectThat(listener.results).hasSize(24)
            expectThat(listener.results.entries.filter { it.value.status == TestExecutionResult.Status.FAILED }
                .map { it.key.displayName }).containsExactlyInAnyOrder("Failing Root Context", "failing context")
        }
    }
}

class TEListener : TestExecutionListener {
    val rootResult = CompletableDeferred<TestExecutionResult>()
    val results = mutableMapOf<TestIdentifier, TestExecutionResult>()
    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        results[testIdentifier] = testExecutionResult
        val parentId = testIdentifier.parentId
        if (parentId.isEmpty)
            rootResult.complete(testExecutionResult)
    }
}


