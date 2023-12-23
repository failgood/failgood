package failgood.internal

import failgood.*
import failgood.ExecutionListener
import failgood.dsl.TestDSL
import failgood.mock.call
import failgood.mock.getCalls
import failgood.mock.mock
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.single

@Test
class TestContextTest {
    val context =
        testsAbout(TestDSL::class) {
            val testDescription =
                TestDescription(Context("root"), "testname", SourceInfo("a", "b", 1))
            val listener = mock<ExecutionListener>()
            val testContext = TestContext(mock(), listener, testDescription, Unit)
            it("publishes a test event for stdout printing") {
                testContext.log("printing to stdout")
                expectThat(getCalls(listener))
                    .single()
                    .isEqualTo(
                        call(
                            ExecutionListener::testEvent,
                            testDescription,
                            "stdout",
                            "printing to stdout"
                        )
                    )
            }
            it("replaces an empty test event to make junit happy") {
                // junit throws when an empty event is published
                testContext._test_event("type", "")
                expectThat(getCalls(listener))
                    .single()
                    .isEqualTo(
                        call(ExecutionListener::testEvent, testDescription, "type", "<empty>")
                    )
            }
            val testName = "tells the name of the test"
            it(testName) {
                assert(testContext.testInfo.name == "testname")
                assert(testInfo.name == testName)
            }
        }
}
