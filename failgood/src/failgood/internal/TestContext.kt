package failgood.internal

import failgood.ExecutionListener
import failgood.TestDescription
import failgood.dsl.ResourcesDSL
import failgood.dsl.TestDSLWithGiven
import failgood.dsl.TestInfo

internal class TestContext<GivenType>(
    resourcesDSL: ResourcesDSL,
    private val listener: ExecutionListener,
    private val testDescription: TestDescription,
    override val given: GivenType
) : TestDSLWithGiven<GivenType>, ResourcesDSL by resourcesDSL {
    override val testInfo: TestInfo = TestInfo(testDescription.testName)

    override suspend fun log(body: String) {
        _test_event("stdout", body)
    }

    override suspend fun _test_event(type: String, body: String) {
        listener.testEvent(testDescription, type, body.ifBlank { "<empty>" })
    }
}
