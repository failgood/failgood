package failgood

import failgood.internal.Colors.RED
import failgood.internal.Colors.RESET
import failgood.internal.ExceptionPrettyPrinter

data class TestPlusResult(val test: TestDescription, val result: TestResult) {
    val isPending = result is Pending
    val isFailed = result is Failed
    val isSuccess = result is Success

    fun prettyPrint(): String {
        val testDescription = test.toString()
        return when (result) {
            is Failed -> {
                val exceptionInfo = ExceptionPrettyPrinter(result.failure, test.stackTraceElement).prettyPrint()

                "$testDescription:$RED failed$RESET with $exceptionInfo.\ntest: ${test.stackTraceElement}"
            }
            is Success -> "$testDescription passed"
            is Pending -> "$testDescription skipped"
        }
    }
}

sealed class TestResult

data class Success(val timeMicro: Long) : TestResult()
object Pending : TestResult()
class Failed(val failure: Throwable) :
    TestResult() {
    override fun equals(other: Any?): Boolean {
        return (other is Failed) && failure.stackTraceToString() == other.failure.stackTraceToString()
    }

    override fun hashCode(): Int = failure.stackTraceToString().hashCode()
    override fun toString(): String {
        return "failed: " + failure.stackTraceToString()
    }
}
