package failgood.functional

import failgood.ExecutionListener
import failgood.Suite
import failgood.Test
import failgood.describe
import failgood.experiments.assertsuccess.assert
import failgood.experiments.assertsuccess.isCallTo
import failgood.mock.getCalls
import failgood.mock.mock

@Test
object EventsTest {
    val tests = describe("Events callbacks") {
        val suite = Suite(
            failgood.describe("rootContext") {
                describe("child") {
                    it("test1") { }
                    it("test2") { }
                }
            }
        )
        describe("event order", isolation = false) {
            val listener = mock<ExecutionListener>()
            suite.run(silent = true, listener = listener)
            val calls = getCalls(listener)
            it("first event is context discovered event for root context") {
                val context = assert(calls.first().isCallTo(ExecutionListener::contextDiscovered))
                assert(context.name == "rootContext")
            }
            it("reports context discovered for every subcontext") {
                val context = assert(calls[1].isCallTo(ExecutionListener::contextDiscovered))
                assert(context.name == "child")
            }
            it("reports test discovered for every test") {
                val context = assert(calls[2].isCallTo(ExecutionListener::testDiscovered))
                assert(context.testName == "test1")
            }
        }
    }
}
