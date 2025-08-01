package failgood.internal

import failgood.Context
import failgood.ExecutionListener
import failgood.SourceInfo
import failgood.Test
import failgood.TestDescription
import failgood.dsl.TestDSL
import failgood.mock.call
import failgood.mock.getCalls
import failgood.mock.mock
import failgood.testCollection

@Test
class TestContextTest {
    val tests =
        testCollection(TestDSL::class) {
            val testDescription =
                TestDescription(Context("root"), "testname", SourceInfo("a", "b", 1))
            val listener = mock<ExecutionListener>()
            val testContext = TestContext(mock(), listener, testDescription, Unit)
            it("publishes a test event for stdout printing") {
                testContext.log("printing to stdout")
                val calls = getCalls(listener)
                assert(calls.size == 1)
                assert(
                    calls.single() ==
                        call(
                            ExecutionListener::testEvent,
                            testDescription,
                            "stdout",
                            "printing to stdout"))
            }
            it("replaces an empty test event to make junit happy") {
                // junit throws when an empty event is published
                testContext._test_event("type", "")
                val calls = getCalls(listener)
                assert(calls.size == 1)
                assert(
                    calls.single() ==
                        call(ExecutionListener::testEvent, testDescription, "type", "<empty>"))
            }
            val testName = "tells the name of the test"
            it(testName) {
                assert(testContext.testInfo.name == "testname")
                assert(testInfo.name == testName)
            }
        }
}
