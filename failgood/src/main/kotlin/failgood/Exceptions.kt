package failgood

open class FailGoodException(override val message: String, override val cause: Throwable? = null) :
    RuntimeException(message, cause)

class SuiteFailedException(reason: String) : FailGoodException(reason)
class EmptySuiteException : FailGoodException("suite can not be empty")
