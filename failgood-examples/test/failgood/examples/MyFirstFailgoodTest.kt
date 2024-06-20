package failgood.examples

import failgood.Test
import failgood.testCollection

@Test
class MyFirstFailgoodTest {
    val tests =
        testCollection("my perfect test suite") {
            it("runs super fast") { assert(true) }
            describe("tests can be organized in subcontexts") { it("just works") {} }
        }
}
