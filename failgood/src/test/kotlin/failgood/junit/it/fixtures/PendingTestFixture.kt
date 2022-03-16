package failgood.junit.it.fixtures

import failgood.describe

object PendingTestFixture {
    private const val ROOT_CONTEXT_NAME = "the root context"
    val context = describe(ROOT_CONTEXT_NAME) {
        pending("pending test")
    }
}
