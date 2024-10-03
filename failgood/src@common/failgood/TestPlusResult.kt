package failgood

data class TestPlusResult(val test: TestDescription, val result: TestResult) {
    val isSkipped: Boolean = result is Skipped
    val isFailure: Boolean = result is Failure
    val isSuccess: Boolean = result is Success
}

internal expect fun TestPlusResult.prettyPrint(): String
