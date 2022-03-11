package failgood.junit.it.fixtures

import failgood.Test
import failgood.describe

@Test
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
