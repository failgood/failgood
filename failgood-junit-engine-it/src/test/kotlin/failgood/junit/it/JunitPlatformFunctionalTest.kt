package failgood.junit.it

import failgood.Test
import failgood.describe
import failgood.junit.FailGoodJunitTestEngine
import failgood.junit.it.fixtures.DuplicateTestNameTest
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import kotlin.reflect.KClass

fun launcherDiscoveryRequest(kClass: KClass<*>): LauncherDiscoveryRequest {
    return LauncherDiscoveryRequestBuilder.request()
        .filters(EngineFilter.includeEngines(FailGoodJunitTestEngine().id))
        .selectors(DiscoverySelectors.selectClass(kClass.qualifiedName))
        .build()
}

@Test
class JunitPlatformFunctionalTest {
    val context = describe("The Junit Platform Engine") {
        it("can execute tests") {
            // just check that it does not crash
            val kClass = DuplicateTestNameTest::class
            val discoveryRequest: LauncherDiscoveryRequest = launcherDiscoveryRequest(kClass)

            LauncherFactory.create().execute(discoveryRequest)
        }
    }

}


