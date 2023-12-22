package failgood.examples

import failgood.Test
import failgood.describe
import failgood.tests

@Test
class MyFirstFailgoodTest {
    val context =
        tests("my perfect test suite") {
            it("runs super fast") { assert(true) }
            describe("tests can be organized in subcontexts") { it("just works") {} }
        }
}
