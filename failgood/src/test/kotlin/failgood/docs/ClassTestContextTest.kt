package failgood.docs

import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
@Suppress("unused")
class ClassTestContextTest {
    // contexts can be declared in fields of type RootContext (what describe returns)
    val context =
        describe("test context defined in a kotlin class") {
            it("describes behavior") {
                expectThat("test").isEqualTo("test")
            }
        }
    val context2 =
        describe("another test context defined in a kotlin class") {
            it("describes behavior") {
                expectThat("test").isEqualTo("test")
            }
        }

    // contexts can also be defined via a method returning a context
    fun methodReturningContext() = describe("a test context returned by a function") {
        it("describes behavior") {
            expectThat("test").isEqualTo("test")
        }
    }

}
