package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testCollection

@TestFixture
class DeeplyNestedDuplicateTestFixture {
    val tests =
        testCollection("1") {
            describe("2") {
                describe("3") {
                    describe("4") { it("5") {} }

                    it("duplicate") {}
                    it("duplicate") {}
                }
            }
        }
}
