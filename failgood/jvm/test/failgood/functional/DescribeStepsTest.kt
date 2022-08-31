package failgood.functional

import failgood.Suite
import failgood.Test
import failgood.describe
import kotlin.test.fail

@Test
class DescribeStepsTest {
    val context = describe("describeSteps", disabled = true) {
        describe("when all tests pass", isolation = false) {
            var finalStep = 0
            val suiteResult = Suite(
                failgood.describe("root") {
                    describeSteps("sequential steps") {
                        var step = 0
                        it("starts with step 0") {
                            assert(step == 0)
                            step = 1
                        }
                        it("executes the second test with the state that the first test leaves") {
                            assert(step == 1)
                            step = 2
                        }
                        it("does the same with the third test") {
                            assert(step == 2)
                            finalStep = step
                        }
                    }
                }
            ).run(silent = true)

            it("calls tests sequentially") {
                assert(suiteResult.allOk)
                assert(finalStep == 2)
            }
            it("reports all tests") {
                assert(
                    suiteResult.allTests.map { it.test.testName } == listOf(
                        "starts with step 0",
                        "executes the second test with the state that the first test leaves",
                        "does the same with the third test"
                    )
                )
            }
        }
        describe("lifecycle") {
            describe("with isolation on") {
                describe("a describeSpecs block is treated as one test") {
                    it("calls after each after each describeSpecs block") {
                    }
                    it("calls autoClose after each describeSpecs block") {
                    }
                }
            }
            describe("with isolation off") {
                it("calls after each after each describeSpecs block") {
                }
                it("calls autoClose just once") {
                }
            }
        }
        describe("when a test fails", isolation = false) {
            val suiteResult = Suite(
                failgood.describe("root") {
                    describeSteps("sequential steps with errors") {
                        it("successful test") {
                        }
                        it("failing test") {
                            fail()
                        }
                        it("test after failing test") {
                        }
                    }
                }
            ).run(silent = true)
            it("reports the failing test as failing") {
                assert(!suiteResult.allOk)
            }
            it("reports the test after the failing test as ignored") {
            }
        }
    }
}
