package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testsAbout

@TestFixture
class DeeplyNestedDuplicateTestFixture {
    val tests =
        testsAbout("1") {
            describe("2") {
                describe("3") {
                    describe("4") { it("5") {} }

                    it("duplicate") {}
                    it("duplicate") {}
                }
            }
        }
}
