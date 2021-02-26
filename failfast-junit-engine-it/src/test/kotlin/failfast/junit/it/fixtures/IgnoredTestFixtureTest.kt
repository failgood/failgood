package failfast.junit.it.fixtures

import failfast.describe

object IgnoredTestFixtureTest {
    const val ROOT_CONTEXT_NAME = "the root context"
    val context = describe(ROOT_CONTEXT_NAME) {
        itWill("pending test")
    }
}
