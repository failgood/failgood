package failgood

open class FailGoodException(override val message: String, override val cause: Throwable? = null) :
    RuntimeException(message, cause)

internal class SuiteFailedException(reason: String) : FailGoodException(reason)
internal class EmptySuiteException : FailGoodException("suite can not be empty")
