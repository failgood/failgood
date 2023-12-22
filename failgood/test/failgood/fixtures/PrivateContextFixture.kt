package failgood.fixtures

import failgood.RootContext
import failgood.describe
import failgood.tests

class PrivateContextFixture {
    private val otherContext = tests("context fixture") {}
    val context: RootContext = tests {
        it("test") {
            val testContext = otherContext
            @Suppress("SENSELESS_COMPARISON") assert(testContext != null)
        }
    }
}
