package failgood.junit.it

import failgood.Test
import failgood.describe
import failgood.junit.FailGoodJunitTestEngine
import failgood.junit.it.fixtures.DuplicateTestNameTest
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory

fun launcherDiscoveryRequest(selector: DiscoverySelector): LauncherDiscoveryRequest {
    return LauncherDiscoveryRequestBuilder.request()
        .filters(EngineFilter.includeEngines(FailGoodJunitTestEngine().id))
        .selectors(selector)
        .build()
}

@Test
class JunitPlatformFunctionalTest {
    val context = describe("The Junit Platform Engine") {
        it("can execute test in a class") {
            LauncherFactory.create().execute(
                launcherDiscoveryRequest(
                    DiscoverySelectors.selectClass(
                        DuplicateTestNameTest::class.qualifiedName
                    )
                )
            )
        }
    }
}


