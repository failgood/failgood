package failgood.fixtures

import failgood.RootContext
import failgood.describe

class PrivateContextFixture {
    private val otherContext = describe("context fixture") {}
    val context: RootContext =
        describe(RootContext::class) {
            it("test") {
                val testContext = otherContext
                @Suppress("SENSELESS_COMPARISON") assert(testContext != null)
            }
        }
}
