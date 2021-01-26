package failfast.internal

import failfast.describe
import strikt.api.expectThat
import strikt.assertions.startsWith
import strikt.assertions.trim

object ExceptionPrettyPrinterTest {
    val context =
        describe(ExceptionPrettyPrinter::class) {
            val assertionError = AssertionError("message")
            val epp = ExceptionPrettyPrinter(assertionError)
            it("pretty prints the exception with stack trace") {
                expectThat(epp.prettyPrint()) {
                    startsWith(assertionError.javaClass.name) // first line

                    // last line
                    get { split("\n").last() }.trim().startsWith("at failfast")
                }
            }
            it("shortens the stack trace") {
                expectThat(epp.stackTrace.last().className).startsWith("failfast")
            }
            itWill("only print the stacktrace lines that correspond to the test case")
            itWill("ignore the exception class name if it is an exception")
        }
}
