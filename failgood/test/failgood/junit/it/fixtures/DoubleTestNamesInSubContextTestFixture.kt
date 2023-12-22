package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture
import failgood.tests

@TestFixture
class DoubleTestNamesInSubContextTestFixture {
    val context =
        tests("failing tests") {
            describe("subcontext") {
                it("test") {}
                it("test") {}
            }
        }
}
