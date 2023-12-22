package failgood.functional

import failgood.ExecutionListener
import failgood.Suite
import failgood.Test
import failgood.describe
import failgood.experiments.assertsuccess.assert
import failgood.experiments.assertsuccess.isCallTo
import failgood.mock.getCalls
import failgood.mock.mock
import failgood.tests

@Test
object EventsTest {
    val tests =
        tests("Events callbacks") {
            val suite = Suite {
                describe("child") {
                    it("test1") {}
                    it("test2") {}
                }
            }
            describe("event order", isolation = false) {
                val listener = mock<ExecutionListener>()
                suite.run(silent = true, listener = listener)
                val calls = getCalls(listener)
                it("first event is context discovered event for root context") {
                    with(assert(calls.first().isCallTo(ExecutionListener::contextDiscovered))) {
                        assert(name == "root")
                    }
                }
                it("reports context discovered for every subcontext") {
                    with(assert(calls[1].isCallTo(ExecutionListener::contextDiscovered))) {
                        assert(name == "child")
                    }
                }
                it("reports test discovered for every test") {
                    assert(
                        calls.getCalls(ExecutionListener::testDiscovered).map { it.testName } ==
                                listOf("test1", "test2")
                    )
                }
            }
        }
}
