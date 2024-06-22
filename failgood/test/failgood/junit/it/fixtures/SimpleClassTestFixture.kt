package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testCollection

@TestFixture
class SimpleClassTestFixture {
    val tests = testCollection("the root context (with brackets)") { it("the test name") {} }
}
