package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testCollection

@TestFixture
class DoubleTestNamesInSubContextTestFixture {
    val tests =
        testCollection("failing tests") {
            describe("subcontext") {
                it("test") {}
                it("test") {}
            }
        }
}
