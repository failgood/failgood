package failgood.docs

import failgood.Test
import failgood.testsAbout
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
@Suppress("unused")
class ClassTestContextExample {
    // contexts can be declared as fields of type RootContext (what describe returns)
    val tests =
        testsAbout("test context defined in a kotlin class") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        }
    val moreTests =
        testsAbout("another test context defined in a kotlin class") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        }

    // contexts can also be defined via a method returning a context
    fun tests() =
        testsAbout("a test context returned by a function") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        }
}
