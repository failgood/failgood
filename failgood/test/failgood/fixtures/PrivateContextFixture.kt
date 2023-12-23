package failgood.fixtures

import failgood.RootContext
import failgood.tests
import failgood.testsAbout

class PrivateContextFixture {
    private val otherContext = testsAbout("context fixture") {}
    val context: RootContext = tests {
        it("test") {
            val testContext = otherContext
            @Suppress("SENSELESS_COMPARISON") assert(testContext != null)
        }
    }
}
