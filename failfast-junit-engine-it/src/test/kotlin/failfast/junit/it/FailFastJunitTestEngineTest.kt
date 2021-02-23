package failfast.junit.it

import failfast.describe
import failfast.junit.FailFastJunitTestEngine
import org.junit.platform.engine.UniqueId
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import strikt.assertions.single

object FailFastJunitTestEngineTest {
    val context = describe(FailFastJunitTestEngine::class) {
        val engine = FailFastJunitTestEngine()
        describe("can discover tests") {
            val testDescriptor =
                engine.discover(launcherDiscoveryRequest(MyTest::class), UniqueId.forEngine(engine.id))
            it("returns a root descriptor") {
                expectThat(testDescriptor.isRoot)
                expectThat(testDescriptor.displayName).isEqualTo("FailFast")
            }
            it("returns all root contexts") {
                expectThat(testDescriptor.children).single().and {
                    get { isContainer }.isTrue()
                    get { displayName }.isEqualTo(MyTest.ROOT_CONTEXT_NAME)
                }
            }

        }
    }
}
