package failgood

import failgood.internal.Colors.RED
import failgood.internal.Colors.RESET
import failgood.internal.ExceptionPrettyPrinter

fun TestPlusResult.prettyPrint(): String {
    val testDescription = test.toString()
    return when (result) {
        is Failure -> {
            val exceptionInfo = ExceptionPrettyPrinter(result.failure, test.sourceInfo).prettyPrint()

            "$testDescription:$RED failed$RESET with $exceptionInfo.\ntest: ${test.sourceInfo}"
        }
        is Success -> "$testDescription passed"
        is Skipped -> "$testDescription skipped"
    }
}
