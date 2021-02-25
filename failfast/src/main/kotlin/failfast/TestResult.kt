package failfast

import failfast.internal.Colors.RED
import failfast.internal.Colors.RESET
import failfast.internal.ExceptionPrettyPrinter
data class TestPlusResult(val test: TestDescription, val result: TestResult)
sealed class TestResult {
    abstract val test: TestDescription
}

data class Success(override val test: TestDescription, val timeMicro: Long) : TestResult()
data class Ignored(override val test: TestDescription) : TestResult()
class Failed(override val test: TestDescription, val failure: Throwable) :
    TestResult() {
    override fun equals(other: Any?): Boolean {
        return (other is Failed) && test == other.test &&
                failure.stackTraceToString() == other.failure.stackTraceToString()
    }

    override fun hashCode(): Int = test.hashCode() * 31 + failure.stackTraceToString().hashCode()
    fun prettyPrint(): String {
        val testDescription = test.toString()
        val exceptionInfo = ExceptionPrettyPrinter(failure, test.stackTraceElement).prettyPrint()

        return "$testDescription:$RED failed$RESET with $exceptionInfo.\ntest: ${test.stackTraceElement}"
    }

}
