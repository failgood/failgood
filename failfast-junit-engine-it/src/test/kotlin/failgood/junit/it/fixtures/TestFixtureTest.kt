package failgood.junit.it.fixtures

import failgood.describe

object TestFixtureTest {
    const val ROOT_CONTEXT_NAME = "the root context"
    val context = describe(ROOT_CONTEXT_NAME) {
        it("test") {}
    }
}
