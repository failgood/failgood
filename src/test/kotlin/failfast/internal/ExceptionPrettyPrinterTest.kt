package failfast.internal

import failfast.describe
import strikt.api.expectThat
import strikt.assertions.matches
import strikt.assertions.startsWith

object ExceptionPrettyPrinterTest {
    val context = describe(ExceptionPrettyPrinter::class) {
        test("shortens assertion errors") {
            val epp = ExceptionPrettyPrinter()
            expectThat(epp.prettyPrint(AssertionError("cause"))) {
                matches(Regex(".*FailFast.kt:\\d*\\)$", RegexOption.DOT_MATCHES_ALL))
                startsWith("cause")

            }
        }
    }
}

