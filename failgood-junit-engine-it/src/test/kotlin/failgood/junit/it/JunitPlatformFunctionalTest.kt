package failgood.junit.it

import failgood.Test
import failgood.describe
import failgood.junit.FailGoodJunitTestEngine
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_TEST_CLASS_SUFFIX
import failgood.junit.it.fixtures.DuplicateTestNameTest
import failgood.junit.it.fixtures.FailingRootContext
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
import strikt.assertions.isEqualTo

fun launcherDiscoveryRequest(selectors: List<DiscoverySelector>, config: Map<String, String> = mapOf() ): LauncherDiscoveryRequest {
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
                    listOf(selectClass(
                        DuplicateTestNameTest::class.qualifiedName
                    ))
                ), listener
            )
            expectThat(listener.rootResult.await()).get { status }.isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
        }
        it("works for a failing root context") {
            val listener = TEListener()
            LauncherFactory.create().execute(
                launcherDiscoveryRequest(listOf(
                    selectClass(FailingRootContext::class.qualifiedName),
                    selectClass(DuplicateTestNameTest::class.qualifiedName)
                ), mapOf(CONFIG_KEY_TEST_CLASS_SUFFIX to "")), listener
            )
            expectThat(listener.rootResult.await()) {
                get { status }.isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
            }
        }
    }
}

class TEListener : TestExecutionListener {
    val rootResult = CompletableDeferred<TestExecutionResult>()
    override fun executionFinished(testIdentifier: TestIdentifier?, testExecutionResult: TestExecutionResult?) {
        val parentId = testIdentifier!!.parentId
        if (parentId.isEmpty)
            rootResult.complete(testExecutionResult!!)
    }
}


