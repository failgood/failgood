package failgood

import failgood.internal.Colors
import failgood.internal.ExceptionPrettyPrinter

actual internal fun TestPlusResult.prettyPrint(): String {
    val testDescription = test.niceString()
    return when (result) {
        is Failure -> {
            val exceptionInfo =
                ExceptionPrettyPrinter(result.failure, test.sourceInfo).prettyPrint()

            "$testDescription:${Colors.RED} failed${Colors.RESET} with $exceptionInfo.\ntest: ${test.sourceInfo}"
        }
        is Success -> "$testDescription passed"
        is Skipped -> "$testDescription skipped"
    }
}
