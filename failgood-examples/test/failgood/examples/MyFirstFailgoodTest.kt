package failgood.examples

import failgood.Test
import failgood.testsAbout

@Test
class MyFirstFailgoodTest {
    val tests =
        testsAbout("my perfect test suite") {
            it("runs super fast") { assert(true) }
            describe("tests can be organized in subcontexts") { it("just works") {} }
        }
}
