package failgood

import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class DescribeTest {
    val context =
        tests("The describe top level method") {
            it("creates a context named '<className>' when called with a class") {
                expectThat(describe(String::class) {}) { get { context.name }.isEqualTo("String") }
            }
        }
}
