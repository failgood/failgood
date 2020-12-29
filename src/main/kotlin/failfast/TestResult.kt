package failfast

sealed class TestResult {
    abstract val test: TestDescriptor
}

data class Success(override val test: TestDescriptor) : TestResult()
data class Ignored(override val test: TestDescriptor) : TestResult()
class Failed(override val test: TestDescriptor, val failure: Throwable) : TestResult() {
    override fun equals(other: Any?): Boolean {
        return (other is Failed)
                && test == other.test
                && failure.stackTraceToString() == other.failure.stackTraceToString()
    }

    override fun hashCode(): Int = test.hashCode() * 31 + failure.stackTraceToString().hashCode()
}
