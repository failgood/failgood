package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture

@TestFixture
class DoubleTestNamesInSubContextTestFixture {
    val context = describe("failing tests") {
        describe("subcontext") {
            it("test") {
            }
            it("test") {
            }
        }
    }
}
