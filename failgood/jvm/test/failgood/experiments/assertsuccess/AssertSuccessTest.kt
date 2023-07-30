package failgood.experiments.assertsuccess

import failgood.Context
import failgood.ExecutionListener
import failgood.SourceInfo
import failgood.Test
import failgood.describe
import failgood.mock.getCalls
import failgood.mock.mock

@Test
object AssertSuccessTest {
    val context = describe("assertSuccess") {
        describe("is useful for asserting on mocks") {
            val listener = mock<ExecutionListener>()
            listener.contextDiscovered(Context("name", sourceInfo = SourceInfo("blah", null, 1)))
            it("can return parameters for further asserting") {
                with(assert(getCalls(listener).first().isCallTo(ExecutionListener::contextDiscovered))) {
                    assert(name == "name")
                }
            }
        }
    }
}
