package nanotest

import strikt.api.expectThat
import strikt.assertions.matches
import strikt.assertions.startsWith

object ExceptionPrettyPrinterTest {
    val context = context {
        test("shortens assertion errors") {
            val epp = ExceptionPrettyPrinter()
            expectThat(epp.prettyPrint(AssertionError("cause"))) {
                matches(Regex(".*NanoTest.kt:\\d*\\)$", RegexOption.DOT_MATCHES_ALL))
                startsWith("cause")

            }
        }
    }
}

