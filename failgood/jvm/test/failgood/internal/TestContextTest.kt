package failgood.internal

import failgood.Context
import failgood.ExecutionListener
import failgood.SourceInfo
import failgood.Test
import failgood.TestDSL
import failgood.TestDescription
import failgood.describe
import failgood.mock.call
import failgood.mock.getCalls
import failgood.mock.mock
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.single

@Test
class TestContextTest {
    val context = describe(TestDSL::class) {
        val testDescription = TestDescription(Context("root"), "testname", SourceInfo("a", "b", 1))
        val listener = mock<ExecutionListener>()
        val testContext = TestContext(mock(), listener, testDescription)
        it("publishes a test event for stdout printing") {
            testContext.println("printing to stdout")
            expectThat(getCalls(listener)).single()
                .isEqualTo(call(ExecutionListener::testEvent, testDescription, "stdout", "printing to stdout"))
        }
        val testName = "tells the name of the test"
        it(testName) {
            assert(testContext.testInfo.name == "testname")
            assert(testInfo.name == testName)
        }
    }
}
