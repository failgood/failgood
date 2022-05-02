package failgood.docs

import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
@Suppress("unused")
class ClassTestContextTest {
    // contexts can be declared in fields of type RootContext (what describe returns)
    val tests =
        describe("test context defined in a kotlin class") {
            it("describes behavior") {
                expectThat("test").isEqualTo("test")
            }
        }
    val moreTests =
        describe("another test context defined in a kotlin class") {
            it("describes behavior") {
                expectThat("test").isEqualTo("test")
            }
        }

    // contexts can also be defined via a method returning a context
    fun tests() = describe("a test context returned by a function") {
        it("describes behavior") {
            expectThat("test").isEqualTo("test")
        }
    }
}
