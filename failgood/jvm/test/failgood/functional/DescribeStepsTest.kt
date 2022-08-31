package failgood.functional

import failgood.Suite
import failgood.Test
import failgood.describe

@Test
class DescribeStepsTest {
    val context = describe("describeSteps", disabled = true) {
        it("has an api") {
            var finalStep = 0
            Suite(
                failgood.describe("root") {
                    describeSteps("sequential steps") {
                        var step = 0
                        it("starts with step 0") {
                            assert(step == 0)
                            step = 1
                        }
                        it("then it is 1") {
                            assert(step == 1)
                            step = 2
                        }
                        it("and then 2") {
                            assert(step == 2)
                            finalStep = step
                        }
                    }
                }
            ).run(silent = true)
            assert(finalStep == 2)
        }
    }
}
