package failgood.docs

import failgood.Test
import failgood.describe
import failgood.tests
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
@Suppress("unused")
class ClassTestContextExample {
    // contexts can be declared as fields of type RootContext (what describe returns)
    val tests =
        tests("test context defined in a kotlin class") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        }
    val moreTests =
        tests("another test context defined in a kotlin class") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        }

    // contexts can also be defined via a method returning a context
    fun tests() =
        tests("a test context returned by a function") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        }
}
