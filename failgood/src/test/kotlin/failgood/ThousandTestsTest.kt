package failgood

import strikt.api.expectThat
import strikt.assertions.isTrue

@Test
class ThousandTestsTest {
    val context = describe("a test suite with 1000 tests in one context") {
        test("runs pretty fast") {
            expectThat(
                RootContext("the context") {
                    repeat(1000) {
                        test("test $it") {
                        }
                    }
                }.toSuite().run(silent = true)
            ).get { allOk }.isTrue()
        }
    }
}
