package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture

@TestFixture
class SimpleClassTestFixture {
    val context = describe("the root context (with brackets)") { it("the test name") {} }
}
