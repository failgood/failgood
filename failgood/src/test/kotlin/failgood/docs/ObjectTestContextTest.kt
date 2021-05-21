package failgood.docs

import failgood.describe
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Testable
class ObjectTestContextTest {
    val context =
        describe("test context defined in a kotlin object") {
            it("describes behavior") { expectThat("test").isEqualTo("test") }
        }
}
