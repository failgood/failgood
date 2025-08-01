package failgood.docs

import failgood.Test
import failgood.testCollection

@Test
@Suppress("unused")
class ClassTestContextExample {
    // contexts can be declared as fields of type RootContext (what describe returns)
    val tests =
        testCollection("test context defined in a kotlin class") {
            it("describes behavior") { assert("test" == "test") }
        }
    val moreTests =
        testCollection("another test context defined in a kotlin class") {
            it("describes behavior") { assert("test" == "test") }
        }

    // contexts can also be defined via a method returning a context
    fun tests() =
        testCollection("a test context returned by a function") {
            it("describes behavior") { assert("test" == "test") }
        }
}
