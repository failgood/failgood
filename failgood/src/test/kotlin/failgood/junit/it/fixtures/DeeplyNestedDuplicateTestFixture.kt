package failgood.junit.it.fixtures

import failgood.Test
import failgood.describe

@Test
class DeeplyNestedDuplicateTestFixture {
    val context = describe("1") {
        describe("2") {
            describe("3") {
                describe("4") {
                    it("5") {
                    }
                }

                it("duplicate") {
                }
                it("duplicate") {
                }
            }
        }
    }
}
