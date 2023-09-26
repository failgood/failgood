package failgood

data class TestPlusResult(val test: TestDescription, val result: TestResult) {
    val isSkipped = result is Skipped
    val isFailure = result is Failure
    val isSuccess = result is Success
}

sealed class TestResult

data class Success(val timeMicro: Long) : TestResult()
internal class Skipped(val reason: String) : TestResult()
class Failure(val failure: Throwable) :
    TestResult() {
        override fun equals(other: Any?): Boolean {
            return (other is Failure) && failure.stackTraceToString() == other.failure.stackTraceToString()
        }

        override fun hashCode(): Int = failure.stackTraceToString().hashCode()
        override fun toString(): String {
            return "failed: " + failure.stackTraceToString()
        }
    }
