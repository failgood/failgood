package failgood

import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class DescribeTest {
    val context = describe("The describe top level method") {
        it("creates a context named '<className>' when called with a class") {
            expectThat(describe(String::class) {}) {
                get { name }.isEqualTo("String")
            }
        }
    }
}
