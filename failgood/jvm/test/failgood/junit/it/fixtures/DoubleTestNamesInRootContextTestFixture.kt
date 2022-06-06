package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture

@TestFixture
class DoubleTestNamesInRootContextTestFixture {
    val context = describe("failing tests") {
        it("test") {
        }
        it("test") {
        }
    }
}
