package failgood.junit.it.fixtures

import failgood.describe

object TestFixture {
    const val ROOT_CONTEXT_NAME = "the root context"
    val testName = "the test name"
    val context = describe(ROOT_CONTEXT_NAME) {
        it(testName) {}
    }
}
