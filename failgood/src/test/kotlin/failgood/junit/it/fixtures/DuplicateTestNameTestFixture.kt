package failgood.junit.it.fixtures

import failgood.Test
import failgood.describe

const val ROOT_CONTEXT_NAME = "the root context"

@Test
class DuplicateTestNameTest {
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
