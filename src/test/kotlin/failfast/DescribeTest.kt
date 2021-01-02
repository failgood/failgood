package failfast

object DescribeTest {
    val context = describe("The test runner") {
        it("supports describe/it syntax") {}
        context("contexts can be nested") {
            test("sub-contexts also contain tests") {}
            test("disabled test")
            itWill("itWill indicates not currently implemented features")
            itWill("itWill can have a body that will not be called") {
            }
        }
    }
}
