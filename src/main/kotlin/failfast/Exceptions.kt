package failfast

open class FailFastException(override val message: String) : RuntimeException(message)
class SuiteFailedException : FailFastException("test failed")
class EmptySuiteException : FailFastException("suite can not be empty")
