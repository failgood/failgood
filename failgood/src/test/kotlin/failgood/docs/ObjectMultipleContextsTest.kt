package failgood.docs

import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class ObjectMultipleContextsTest {
    val context = listOf(
        describe("first of multiple contexts defined in one object") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        },
        describe("second of multiple contexts defined in one object") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        }
    )
}
