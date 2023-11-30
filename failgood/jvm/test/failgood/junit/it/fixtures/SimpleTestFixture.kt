package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture

@TestFixture
object SimpleTestFixture {
    const val ROOT_CONTEXT_NAME = "the root context (with brackets)"
    val context = describe(ROOT_CONTEXT_NAME) { it("the test name") {} }
}
