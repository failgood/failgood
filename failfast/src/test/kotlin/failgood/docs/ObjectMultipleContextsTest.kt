package failgood.docs

import failgood.describe
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Testable
class ObjectMultipleContextsTest {
    val context = listOf(
        describe("first of multiple contexts defined in one object") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        },
        describe("second of multiple contexts defined in one object") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        })
}
