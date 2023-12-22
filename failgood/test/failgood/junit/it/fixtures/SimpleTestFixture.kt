package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture
import failgood.tests

@TestFixture
object SimpleTestFixture {
    const val ROOT_CONTEXT_NAME = "the root context (with brackets)"
    val context = tests(ROOT_CONTEXT_NAME) { it("the test name") {} }
}
