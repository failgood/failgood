package failfast

import failfast.internal.Colors.RED
import failfast.internal.Colors.RESET
import failfast.internal.ExceptionPrettyPrinter

data class TestPlusResult(val test: TestDescription, val result: TestResult) {
    val isIgnored = result is Ignored
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
            is Ignored -> "$testDescription skipped"
        }
    }
}

sealed class TestResult {
    abstract val test: TestDescription
}

data class Success(override val test: TestDescription, val timeMicro: Long) : TestResult()
data class Ignored(override val test: TestDescription) : TestResult()
class Failed(override val test: TestDescription, val failure: Throwable) :
    TestResult() {
    override fun equals(other: Any?): Boolean {
        return (other is Failed) && failure.stackTraceToString() == other.failure.stackTraceToString()
    }

    override fun hashCode(): Int = failure.stackTraceToString().hashCode()
}
