package failgood.examples

import failgood.Test
import failgood.describe

@Test
class MyFirstFailgoodTest {
    val context =
        describe("my perfect test suite") {
            it("runs super fast") { assert(true) }
            describe("tests can be organized in subcontexts") { it("just works") {} }
        }
}
