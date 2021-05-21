package failgood

open class FailGoodException(override val message: String) : RuntimeException(message)
class SuiteFailedException(reason: String) : FailGoodException(reason)
class EmptySuiteException : FailGoodException("suite can not be empty")
