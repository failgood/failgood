@file:Suppress("NAME_SHADOWING")

package failgood.internal

import failgood.describe
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.allIndexed
import strikt.assertions.contains
import strikt.assertions.startsWith
import strikt.assertions.trim

@Testable
class ExceptionPrettyPrinterTest {
    val context =
        describe(ExceptionPrettyPrinter::class) {
            val assertionError = AssertionError("message")
            val epp = ExceptionPrettyPrinter(assertionError)
            it("pretty prints the exception with stack trace") {
                expectThat(epp.prettyPrint()) {
                    startsWith("message") // first line

                    // last line
                    get { split("\n").last() }.trim().startsWith("at failgood")
                }
            }
            it("shows only stack trace lines for assertion errors that are in the test file") {
                val assertionError = AssertionError("message")
                val stackTraceElement =
                    assertionError.stackTrace.first { it.className.startsWith(ExceptionPrettyPrinterTest::class.qualifiedName!!) }
                expectThat(
                    ExceptionPrettyPrinter(assertionError, stackTraceElement).prettyPrint().split("\n")
                ).allIndexed { idx ->
                    if (idx == 0) contains("message")
                    else
                        contains(stackTraceElement.className)
                }
            }
            it("shortens the stack trace") {
                expectThat(epp.stackTrace.last().className).startsWith("failgood")
            }
        }
}
