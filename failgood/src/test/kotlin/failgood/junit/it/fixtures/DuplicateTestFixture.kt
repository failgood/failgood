package failgood.junit.it.fixtures

import failgood.Test
import failgood.describe

@Test
class DuplicateTestFixture {
    val context = describe("closing test resources") {
        describe("autoclosable") {
            it("is closed in reverse order of creation") {
            }
            it("closes autocloseables without callback") {
            }
            it("works inside a test") {
            }
            describe("error handling") {
                describe("when the test fails and autoclose and aftereach work") {
                    it("calls autoclose callbacks") {
                    }
                    it("calls afterEach callbacks") {
                    }
                    it("reports the test failure") {
                    }
                }

                it("reports the test failure even when the close callback fails too") {
                }
                it("reports the test failure even when the close callback fails too") {
                }
            }
        }
        describe("after suite callback") {
            it("is called exactly once at the end of the suite, after all tests are finished") {
            }
            it("can throw exceptions that are ignored") {
            }
        }
    }
}
