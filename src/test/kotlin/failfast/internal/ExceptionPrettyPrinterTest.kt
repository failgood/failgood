package failfast.internal

import failfast.describe
import strikt.api.expectThat
import strikt.assertions.matches
import strikt.assertions.startsWith

object ExceptionPrettyPrinterTest {
    val context =
        describe(ExceptionPrettyPrinter::class) {
            test("shortens assertion errors") {
                val assertionError = AssertionError("cause")
                val epp = ExceptionPrettyPrinter(assertionError)
                expectThat(epp.prettyPrint()) {
                    matches(Regex(".*ContextExecutor.kt:\\d*\\)$", RegexOption.DOT_MATCHES_ALL))
                    startsWith(assertionError.javaClass.name)
                }
            }
        }
}
