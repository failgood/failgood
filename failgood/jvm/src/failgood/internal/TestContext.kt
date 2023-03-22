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
    private val testExecutionContext = TestExecutionContext()
    override val testInfo: TestInfo = TestInfo(testDescription.testName, testExecutionContext)

    override suspend fun log(body: String) {
        _test_event("stdout", body)
    }

    override suspend fun _test_event(type: String, body: String) {
        listener.testEvent(testDescription, type, body.ifBlank { "<empty>" })
        testExecutionContext.events.add(TestExecutionContext.Event(type, body))
    }
}
