package failgood.docs

import failgood.Test
import failgood.testsAbout
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
object ObjectTestContextExample {
    val tests =
        testsAbout("test context defined in a kotlin object") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        }
}
