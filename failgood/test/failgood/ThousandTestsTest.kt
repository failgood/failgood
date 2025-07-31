package failgood

@Test
class ThousandTestsTest {
    val tests =
        testCollection("a test suite with 1000 tests in one context") {
            test("runs pretty fast") {
                val result =
                    Suite(TestCollection("the context") { repeat(1000) { test("test $it") {} } })
                        .run(silent = true)
                assert(result.allOk)
            }
        }
}
