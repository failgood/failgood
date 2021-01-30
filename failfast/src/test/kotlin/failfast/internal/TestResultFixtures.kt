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
    val testResults = listOf(
        Success(TestDescription(rootContext, "supports describe/it syntax"), 10),
        Success(
            TestDescription(subContext, "sub-contexts also contain tests"),
            20
        ),
        Failed(
            TestDescription(subContext, "failed test"),
            failure,
            StackTraceElement("ClassName", "method", "file", 123)
        )
    )
}
