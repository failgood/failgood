package failgood

import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class TopLevelTestsMethodsTest {
    val tests =
        testCollection("The testsAbout top level method") {
            it("creates a context named '<className>' when called with a class") {
                expectThat(testCollection(String::class) {}) {
                    get { rootContext.name }.isEqualTo("String")
                }
            }
        }
}
