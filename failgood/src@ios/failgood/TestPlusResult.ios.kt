package failgood

import failgood.internal.Colors

internal actual fun TestPlusResult.prettyPrint(): String {
    val testDescription = test.niceString()
    return when (result) {
        is Failure -> "$testDescription:${Colors.RED} failed${Colors.RESET} with ${result.failure}"
        is Success -> "$testDescription passed"
        is Skipped -> "$testDescription skipped"
    }
}
