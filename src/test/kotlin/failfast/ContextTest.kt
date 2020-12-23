package failfast

import strikt.api.expectThat
import strikt.assertions.isEqualTo

object ContextTest {
    val context = describe("a test context") {
        it("can tell its name with path") {
            val root = Context("root", null)
            val context = Context("name", root)
            expectThat(context.asStringWithPath()).isEqualTo("root > name")
        }
    }
}
