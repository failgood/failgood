package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture
import failgood.tests

@TestFixture
class SimpleClassTestFixture {
    val context = tests("the root context (with brackets)") { it("the test name") {} }
}
