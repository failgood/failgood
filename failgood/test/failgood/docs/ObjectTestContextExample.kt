package failgood.docs

import failgood.Test
import failgood.testCollection

@Test
object ObjectTestContextExample {
    val tests =
        testCollection("test context defined in a kotlin object") {
            it("describes behavior") { assert("test" == "test") }
        }
}
