package failgood

import strikt.api.expectThat
import strikt.assertions.isTrue

@Test
class ThousandTestsTest {
    val tests =
        testCollection("a test suite with 1000 tests in one context") {
            test("runs pretty fast") {
                expectThat(
                        Suite(
                                TestCollection("the context") {
                                    repeat(1000) { test("test $it") {} }
                                })
                            .run(silent = true))
                    .get { allOk }
                    .isTrue()
            }
        }
}
