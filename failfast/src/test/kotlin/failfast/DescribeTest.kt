package failfast

import strikt.api.expectThat
import strikt.assertions.isEqualTo

object DescribeTest {
    val context = describe("The describe top level method") {
        it("creates a context named 'The <className>' when called with a class") {
            expectThat(describe(String::class) {}) {
                get { name }.isEqualTo("The String")
            }
        }
    }
}
