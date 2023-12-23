package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testsAbout

@TestFixture
object SimpleTestFixture {
    const val ROOT_CONTEXT_NAME = "the root context (with brackets)"
    val tests = testsAbout(ROOT_CONTEXT_NAME) { it("the test name") {} }
}
