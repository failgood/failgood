package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture
import failgood.tests

@TestFixture
class DeeplyNestedDuplicateTestFixture {
    val context =
        tests("1") {
            describe("2") {
                describe("3") {
                    describe("4") { it("5") {} }

                    it("duplicate") {}
                    it("duplicate") {}
                }
            }
        }
}
