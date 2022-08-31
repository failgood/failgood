package failgood.functional

import failgood.Suite
import failgood.Test
import failgood.describe

@Test
class DescribeStepsTest {
    val context = describe("describeSteps", disabled = true, isolation = false) {
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
        it("stops execution when one test fails") {}
    }
}
