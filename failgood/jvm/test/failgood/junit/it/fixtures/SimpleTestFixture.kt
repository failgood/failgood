package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture

@TestFixture
object SimpleTestFixture {
    const val ROOT_CONTEXT_NAME = "the root context (with brackets)"
    const val TEST_NAME = "the test name"
    val context = describe(ROOT_CONTEXT_NAME) {
        it(TEST_NAME) {}
    }
}
