package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.tests

@TestFixture
object SimpleUnnamedTestFixtureWithMultipleTests {
    val tests =
        tests {
            it("a test in the root context") {}
            describe("a context in the root context") {
                it("a test in the subcontext") {}
                it("a different test in the subcontext") {}
            }
        }
}
