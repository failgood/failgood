package failgood.functional

import failgood.Context
import failgood.ExecutionListener
import failgood.Suite
import failgood.Test
import failgood.TestDescription
import failgood.describe
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
                val first = calls.first()
                assert(first.function == ExecutionListener::contextDiscovered.name)
                val context = first.arguments.singleOrNull()
                assert(context is Context && context.name == "rootContext")
            }
            it("reports context discovered for every subcontext") {
                val second = calls[1]
                assert(second.function == ExecutionListener::contextDiscovered.name)
                val context = second.arguments.singleOrNull()
                assert(context is Context && context.name == "child")
            }
            it("reports test discovered for every test") {
                val third = calls[2]
                assert(third.function == ExecutionListener::testDiscovered.name)
                val test = third.arguments.singleOrNull()
                assert(test is TestDescription && test.testName == "test1")
            }
        }
    }
}
