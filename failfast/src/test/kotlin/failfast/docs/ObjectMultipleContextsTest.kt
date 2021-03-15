package failfast.docs

import failfast.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

object ObjectMultipleContextsTest {
    val context = listOf(
        describe("first of multiple contexts defined in one object") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        },
        describe("second of multiple contexts defined in one object") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        })
}
