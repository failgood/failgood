package failfast

object DescribeTest {
    val context = describe("The test runner") {
        it("supports describe/it syntax")
        context("contexts can be nested") {
            test("subcontexts also contain tests")
        }
    }
}
