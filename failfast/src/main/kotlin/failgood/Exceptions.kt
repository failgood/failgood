package failgood

open class FailFastException(override val message: String) : RuntimeException(message)
class SuiteFailedException(reason: String) : FailFastException(reason)
class EmptySuiteException : FailFastException("suite can not be empty")
