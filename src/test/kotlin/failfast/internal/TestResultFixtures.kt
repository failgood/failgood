package failfast.internal

import failfast.Context
import failfast.Failed
import failfast.Success
import failfast.TestDescriptor

object TestResultFixtures {
    val rootContext = Context("the test runner", null)
    val subContext = Context("contexts can be nested", rootContext)
    val subSubContext = Context("deeper", subContext)
    val testResults = listOf(
        Success(TestDescriptor(rootContext, "supports describe/it syntax"), 10),
        Success(
            TestDescriptor(subContext, "sub-contexts also contain tests"),
            20
        ),
        Failed(
            TestDescriptor(subContext, "failed test"), RuntimeException("failure message")
        )
    )
}
