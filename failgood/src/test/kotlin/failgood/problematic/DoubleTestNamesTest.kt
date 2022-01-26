package failgood.problematic

import failgood.Test
import failgood.describe

@Test
class DoubleTestNamesTest {
    val context = describe("failing tests") {
        describe("id") {
            it("reports 'success' for an admin user") {
            }
            it("reports 'success' for an admin user") {
            }
        }
    }
}
