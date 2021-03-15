package failfast.junit.it.fixtures

import failfast.describe

object PendingTestFixtureTest {
    const val ROOT_CONTEXT_NAME = "the root context"
    val context = describe(ROOT_CONTEXT_NAME) {
        pending("pending test")
    }
}
