package failfast.junit

import failfast.describe
import org.junit.platform.engine.UniqueId
import strikt.api.expectThat

object FailFastJunitTestEngineTest {
    val context = describe(FailFastJunitTestEngine::class) {
        val engine = FailFastJunitTestEngine()
        describe("can discover tests") {
            val testDescriptor =
                engine.discover(launcherDiscoveryRequest(MyTestClass::class), UniqueId.forEngine(engine.id))
            it("returns a root descriptor") {
                expectThat(testDescriptor.isRoot)
            }
        }
    }
}
