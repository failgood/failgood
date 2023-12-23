package failgood

import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class TopLevelTestsMethodsTest {
    val tests =
        testsAbout("The testsAbout top level method") {
            it("creates a context named '<className>' when called with a class") {
                expectThat(testsAbout(String::class) {}) { get { context.name }.isEqualTo("String") }
            }
        }
}
