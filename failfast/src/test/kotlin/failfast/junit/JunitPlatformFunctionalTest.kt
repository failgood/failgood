package failfast.junit

import failfast.describe
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory

object JunitPlatformFunctionalTest {
    val context = describe("The Junit Platform Engine") {
        it("can execute tests") {
            val discoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .filters(EngineFilter.includeEngines(FailFastJunitTestEngine().id))
                .selectors(DiscoverySelectors.selectClass(MyTestClass::class.qualifiedName))
                .build()

            LauncherFactory.create().execute(discoveryRequest)
        }
    }
}


object MyTestClass {
    val context = describe("the root context") {
        it("contains a test named joseph") {}
        describe("and the sub context") {
            it("also contains a test named joseph") {}
            describe("and another sub context") {
                it("also contains a test named joseph") {}
            }
        }
    }
}
