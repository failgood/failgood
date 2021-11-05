package failgood.junit.it

import failgood.Test
import failgood.describe
import failgood.junit.FailGoodJunitTestEngine
import failgood.junit.it.fixtures.DuplicateTestNameTest
import failgood.junit.it.fixtures.FailingRootContext
import kotlinx.coroutines.CompletableDeferred
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.message

fun launcherDiscoveryRequest(selector: DiscoverySelector): LauncherDiscoveryRequest {
    return LauncherDiscoveryRequestBuilder.request()
        .filters(EngineFilter.includeEngines(FailGoodJunitTestEngine().id))
        .selectors(selector)
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
                    DiscoverySelectors.selectClass(
                        DuplicateTestNameTest::class.qualifiedName
                    )
                ), listener
            )
            expectThat(listener.result.await()).get { status }.isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
        }
        it("works for a failing root context") {
            val listener = TEListener()
            LauncherFactory.create().execute(
                launcherDiscoveryRequest(
                    DiscoverySelectors.selectClass(
                        FailingRootContext::class.qualifiedName
                    )
                ), listener
            )
            expectThat(listener.result.await()) {
                get { status }.isEqualTo(TestExecutionResult.Status.FAILED)
                get { throwable.get() }.message.isEqualTo(FailingRootContext.thrownException.message)
            }
        }
    }
}

class TEListener : TestExecutionListener {
    val result = CompletableDeferred<TestExecutionResult>()
    override fun executionFinished(testIdentifier: TestIdentifier?, testExecutionResult: TestExecutionResult?) {
        result.complete(testExecutionResult!!)
    }
}


