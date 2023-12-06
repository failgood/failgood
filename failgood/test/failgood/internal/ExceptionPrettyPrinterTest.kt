@file:Suppress("NAME_SHADOWING")

package failgood.internal

import failgood.SourceInfo
import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.allIndexed
import strikt.assertions.contains
import strikt.assertions.startsWith
import strikt.assertions.trim

@Test
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
                val prefix = ExceptionPrettyPrinterTest::class.qualifiedName!!
                val sourceInfo =
                    assertionError.stackTrace
                        .first { it.className.startsWith(prefix) }
                        .let { SourceInfo(it.className, it.fileName, it.lineNumber) }
                expectThat(
                        ExceptionPrettyPrinter(assertionError, sourceInfo).prettyPrint().split("\n")
                    )
                    .allIndexed { idx ->
                        if (idx == 0) contains("message") else contains(sourceInfo.className)
                    }
            }
            it("shortens the stack trace") {
                expectThat(epp.stackTrace.last().className).startsWith("failgood")
            }
        }
}
