package failgood.fixtures

import failgood.TestCollection
import failgood.tests
import failgood.testsAbout

class PrivateContextFixture {
    // this testCollection declared in a private val is not going to found or executed
    private val otherContext = testsAbout("context fixture") {}
    val context: TestCollection<Unit> = tests {
        it("test") {
            val testContext = otherContext
            @Suppress("SENSELESS_COMPARISON") assert(testContext != null)
        }
    }
}
