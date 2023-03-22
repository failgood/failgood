package failgood.internal

import failgood.ExecutionListener
import failgood.ResourcesDSL
import failgood.TestDSL
import failgood.TestDescription
import failgood.TestExecutionContext
import failgood.TestInfo

internal class TestContext(
    resourcesDSL: ResourcesDSL,
    private val listener: ExecutionListener,
    private val testDescription: TestDescription
) : TestDSL, ResourcesDSL by resourcesDSL {
    private val testExecutionContext = TestExecutionContext(listener, testDescription)
    override val testInfo: TestInfo = TestInfo(testDescription.testName, testExecutionContext)

    override suspend fun log(body: String) {
        testExecutionContext.event("stdout", body)
    }
}
