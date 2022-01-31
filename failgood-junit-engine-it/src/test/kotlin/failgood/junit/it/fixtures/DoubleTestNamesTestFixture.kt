package failgood.junit.it.fixtures

import failgood.Test
import failgood.describe

@Test
class DoubleTestNamesTestFixture {
    val context = describe("failing tests") {
        it("test") {
        }
        it("test") {
        }
    }
}
