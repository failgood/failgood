@file:Suppress("NAME_SHADOWING")

package failgood.internal

import failgood.SourceInfo
import failgood.Test
import failgood.testCollection

@Test
class ExceptionPrettyPrinterTest {
    val tests =
        testCollection(ExceptionPrettyPrinter::class) {
            val assertionError = AssertionError("message")
            val epp = ExceptionPrettyPrinter(assertionError)
            it("pretty prints the exception with stack trace") {
                val prettyPrint = epp.prettyPrint()
                assert(prettyPrint.startsWith("message")) // first line
                // last line
                assert(prettyPrint.split("\n").last().trim().startsWith("at failgood"))
            }
            it("shows only stack trace lines for assertion errors that are in the test file") {
                val assertionError = AssertionError("message")
                val prefix = ExceptionPrettyPrinterTest::class.qualifiedName!!
                val sourceInfo =
                    assertionError.stackTrace
                        .first { it.className.startsWith(prefix) }
                        .let { SourceInfo(it.className, it.fileName, it.lineNumber) }
                val lines =
                    ExceptionPrettyPrinter(assertionError, sourceInfo).prettyPrint().split("\n")
                lines.forEachIndexed { idx, line ->
                    if (idx == 0) {
                        assert(line.contains("message"))
                    } else {
                        assert(line.contains(sourceInfo.className))
                    }
                }
            }
            it("shortens the stack trace") {
                assert(epp.stackTrace.last().className.startsWith("failgood"))
            }
        }
}
