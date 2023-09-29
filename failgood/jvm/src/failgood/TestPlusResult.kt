package failgood

import failgood.internal.Colors
import failgood.internal.ExceptionPrettyPrinter

data class TestPlusResult(val test: TestDescription, val result: TestResult) {
    val isSkipped = result is Skipped
    val isFailure = result is Failure
    val isSuccess = result is Success

    fun prettyPrint(): String {
        val testDescription = test.toString()
        return when (result) {
            is Failure -> {
                val exceptionInfo = ExceptionPrettyPrinter(result.failure, test.sourceInfo).prettyPrint()

                "$testDescription:${Colors.RED} failed${Colors.RESET} with $exceptionInfo.\ntest: ${test.sourceInfo}"
            }
            is Success -> "$testDescription passed"
            is Skipped -> "$testDescription skipped"
        }
    }
}
