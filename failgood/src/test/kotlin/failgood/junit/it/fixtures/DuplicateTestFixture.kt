package failgood.junit.it.fixtures

import failgood.Test
import failgood.describe

@Test
class DuplicateTestFixture {
    val context = describe("1") {
        describe("2") {
            describe("3") {
                describe("4") {
                    it("5") {
                    }
                    it("6") {
                    }
                    it("7") {
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
