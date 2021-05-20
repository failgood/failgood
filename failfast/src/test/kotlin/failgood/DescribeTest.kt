package failgood

import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Testable
class DescribeTest {
    val context = describe("The describe top level method") {
        it("creates a context named 'The <className>' when called with a class") {
            expectThat(describe(String::class) {}) {
                get { name }.isEqualTo("The String")
            }
        }
    }
}
