package failfast

object FailFastTest {
    val context =
        describe("The test runner") {
            it("supports describe/it syntax") {}
            describe("nested contexts") {
                it("can contain tests too") {}
                itWill("itWill can be used to mark pending tests") {}
                itWill("for pending tests the test body is optional")
                context("context/test syntax is also supported") {
                    test("i prefer describe/it but if there is no subject to describe I use context/test") {
                    }
                    test("tests without body are pending")
                }
            }
        }
}
