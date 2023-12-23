package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testsAbout

@TestFixture
class SimpleClassTestFixture {
    val tests = testsAbout("the root context (with brackets)") { it("the test name") {} }
}
