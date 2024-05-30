package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testsAbout

@TestFixture
class DoubleTestNamesInSubContextTestFixture {
    val tests =
        testsAbout("failing tests") {
            describe("subcontext") {
                it("test") {}
                it("test") {}
            }
        }
}
