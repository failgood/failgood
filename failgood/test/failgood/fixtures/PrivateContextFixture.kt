package failgood.fixtures

import failgood.TestCollection
import failgood.tests
import failgood.testsAbout

class PrivateContextFixture {
    private val otherContext = testsAbout("context fixture") {}
    val context: TestCollection<Unit> = tests {
        it("test") {
            val testContext = otherContext
            @Suppress("SENSELESS_COMPARISON") assert(testContext != null)
        }
    }
}
