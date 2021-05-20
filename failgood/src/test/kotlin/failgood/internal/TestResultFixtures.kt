package failgood.internal

import failgood.*

object TestResultFixtures {
    val rootContext = Context("the test runner", null)
    val subContext = Context("contexts can be nested", rootContext)
    val subSubContext = Context("deeper", subContext)
    val failure = RuntimeException("failure message\nwith newline")
    private val stackTraceElement = StackTraceElement("ClassName", "method", "file", 123)
    val testResults: List<TestPlusResult> = listOf(
        TestPlusResult(
            TestDescription(rootContext, "supports describe/it syntax", stackTraceElement),
            Success(10)
        ),
        TestPlusResult(
            TestDescription(subContext, "sub-contexts also contain tests", stackTraceElement), Success(
                20
            )
        ),
        TestPlusResult(
            TestDescription(subContext, "failed test", stackTraceElement), Failed(
                failure
            )
        )
    )
}
