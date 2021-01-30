package failfast

fun main() {
    FailFast.runTest()
}

object ThousandTestsTest {
    val context = describe("a test suite with 1000 tests in one context") {
        test("runs pretty fast") {
            Suite(describe("the context", false) {
                repeat(1000) {
                    test("test $it") {

                    }
                }
            }).run(silent = true)
        }
    }
}
