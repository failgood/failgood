package failfast

object DescribeTest {
    val context = describe("The test runner") {
        it("supports describe/it syntax") {}
        context("contexts can be nested") {
            test("subcontexts also contain tests") {}
            test("disabled test")
            itWill("itwill indicates not currently implemented features")
            itWill("itwill can have a body that will not be called") {
            }
        }
    }
}
