package failfast.docs

import failfast.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

object ObjectTestContextTest {
    val context = describe("test context defined in a kotlin object") {
        it("describes behavior") {
            expectThat("test").isEqualTo("test")
        }
    }
}
