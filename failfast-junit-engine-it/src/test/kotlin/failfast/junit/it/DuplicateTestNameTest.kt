package failfast.junit.it

import failfast.describe

object DuplicateTestNameTest {
    const val ROOT_CONTEXT_NAME = "the root context"
    val context = describe(ROOT_CONTEXT_NAME) {
        it("contains a test named joseph") {}
        describe("and the sub context") {
            it("also contains a test named joseph") {}
            describe("and another sub context") {
                it("also contains a test named joseph") {}
            }
        }
    }
}
