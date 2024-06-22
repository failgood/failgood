package failgood.fixtures

import failgood.TestCollection
import failgood.testCollection

class PrivateContextFixture {
    // this context is declared in a way that makes the kotlin compiler generate a static synthetic getter
    // probably as an optimization.
    // this testCollection declared in a private val is not going to found or executed.
    private val otherContext = testCollection("context fixture") {}
    val context: TestCollection<Unit> = testCollection {
        it("test") {
            val testContext = otherContext
            @Suppress("SENSELESS_COMPARISON") assert(testContext != null)
        }
    }
}
