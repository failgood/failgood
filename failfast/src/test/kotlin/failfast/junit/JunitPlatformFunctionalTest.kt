package failfast.junit

import failfast.describe
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import kotlin.reflect.KClass

fun launcherDiscoveryRequest(kClass: KClass<MyTestClass>): LauncherDiscoveryRequest {
    return LauncherDiscoveryRequestBuilder.request()
        .filters(EngineFilter.includeEngines(FailFastJunitTestEngine().id))
        .selectors(DiscoverySelectors.selectClass(kClass.qualifiedName))
        .build()
}

object JunitPlatformFunctionalTest {
    val context = describe("The Junit Platform Engine", disabled = true) {
        it("can execute tests") {
            val kClass = MyTestClass::class
            val discoveryRequest: LauncherDiscoveryRequest = launcherDiscoveryRequest(kClass)

            LauncherFactory.create().execute(discoveryRequest)
        }
    }

}


