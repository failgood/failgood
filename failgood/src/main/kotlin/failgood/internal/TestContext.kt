package failgood.internal

import failgood.ExecutionListener
import failgood.ResourcesDSL
import failgood.TestDSL
import failgood.TestDescription

class TestContext(
    resourcesDSL: ResourcesDSL,
    private val listener: ExecutionListener,
    private val testDescription: TestDescription
) : TestDSL, ResourcesDSL by resourcesDSL {
    override suspend fun println(body: String) {
        _test_event("stdout", body)
    }

    override suspend fun _test_event(type: String, body: String) {
        listener.testEvent(testDescription, type, body)
    }
}
