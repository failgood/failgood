package failfast

import strikt.api.expectThat
import strikt.assertions.isEqualTo

object DescribeTest {
    val context = describe("the describe top level method") {
        it("will create a context named the <subject> when called with a class") {
            expectThat(describe(String::class) {}) {
                get { name }.isEqualTo("the String")
            }
        }
    }
}
