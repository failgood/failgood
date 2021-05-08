package failfast.docs

import failfast.describe
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo
class ClassTestContextTest {
    val context =
        describe("test context defined in a kotlin class") {
            it("describes behavior") {
                expectThat("test").isEqualTo("test")
            }
        }
}
