package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture

@TestFixture
object TestFixture {
    const val ROOT_CONTEXT_NAME = "the root context"
    val testName = "the test name"
    val context = describe(ROOT_CONTEXT_NAME) {
        it(testName) {}
    }
}
