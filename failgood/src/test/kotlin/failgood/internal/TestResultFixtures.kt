package failgood.internal

import failgood.Context
import failgood.Failed
import failgood.SourceInfo
import failgood.Success
import failgood.TestDescription
import failgood.TestPlusResult

object TestResultFixtures {
    val rootContext = Context("the test runner", null)
    val subContext = Context("contexts can be nested", rootContext)
    val subSubContext = Context("deeper", subContext)
    val failure = RuntimeException("failure message\nwith newline")
    private val sourceInfo = SourceInfo("ClassName", "file", 123)
    val testResults: List<TestPlusResult> = listOf(
        TestPlusResult(
            TestDescription(rootContext, "supports describe/it syntax", sourceInfo),
            Success(10)
        ),
        TestPlusResult(
            TestDescription(subContext, "sub-contexts also contain tests", sourceInfo),
            Success(
                20
            )
        ),
        TestPlusResult(
            TestDescription(subContext, "failed test", sourceInfo),
            Failed(
                failure
            )
        )
    )
}
