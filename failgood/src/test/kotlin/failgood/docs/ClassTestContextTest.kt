package failgood.docs

import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
@Suppress("unused")
class ClassTestContextTest {
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
}
