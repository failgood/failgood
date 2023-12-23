package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testsAbout

const val ROOT_CONTEXT_NAME = "the root context"

@TestFixture
class DuplicateTestNameTest {
    val tests =
        testsAbout(ROOT_CONTEXT_NAME) {
            it("contains a test named joseph") {}
            describe("and the sub context") {
                it("also contains a test named joseph") {}
                describe("and another sub context") { it("also contains a test named joseph") {} }
            }
        }
}
