package failgood.docs

import failgood.Test
import failgood.testCollection
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
object ObjectTestContextExample {
    val tests =
        testCollection("test context defined in a kotlin object") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        }
}
