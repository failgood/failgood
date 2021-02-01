package failfast.internal

import failfast.Context
import failfast.Failed
import failfast.Success
import failfast.TestDescription

object TestResultFixtures {
    val rootContext = Context("the test runner", null)
    val subContext = Context("contexts can be nested", rootContext)
    val subSubContext = Context("deeper", subContext)
    val failure = RuntimeException("failure message\nwith newline")
    private val stackTraceElement = StackTraceElement("ClassName", "method", "file", 123)
    val testResults = listOf(
        Success(TestDescription(rootContext, "supports describe/it syntax", stackTraceElement), 10),
        Success(
            TestDescription(subContext, "sub-contexts also contain tests", stackTraceElement),
            20
        ),
        Failed(
            TestDescription(subContext, "failed test", stackTraceElement),
            failure
        )
    )
}
