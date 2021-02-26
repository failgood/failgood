package failfast.junit.it.fixtures

import failfast.describe

object TestFixtureTest {
    const val ROOT_CONTEXT_NAME = "the root context"
    val context = describe(ROOT_CONTEXT_NAME) {
        it("test") {}
        itWill("pending test")
    }
}
