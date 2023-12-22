package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture
import failgood.tests

@TestFixture
object SimpleTestFixtureWithMultipleTests {
    val context =
        tests("the root context (with brackets)") {
            it("a test in the root context") {}
            describe("a context in the root context") {
                it("a test in the subcontext") {}
                it("a different test in the subcontext") {}
            }
        }
}
