package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testCollection

@TestFixture
object SimpleUnnamedTestFixtureWithMultipleTests {
    val tests = testCollection {
        it("a test in the root context") {}
        describe("a context in the root context") {
            it("a test in the subcontext") {}
            it("a different test in the subcontext") {}
        }
    }
}
