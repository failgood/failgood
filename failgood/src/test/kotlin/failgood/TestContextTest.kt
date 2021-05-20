package failgood

import failgood.mock.call
import failgood.mock.getCalls
import failgood.mock.mock
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.single

@Testable
class TestContextTest {
    val context = describe(TestDSL::class) {
        it("publishes a test event for stdout printing") {
            val testDescription = TestDescription(Context("root"), "testname", StackTraceElement("a", "b", "c", 1))
            val listener = mock<ExecutionListener>()
            TestContext(mock(), listener, testDescription).println("printing to stdout")
            expectThat(getCalls(listener)).single()
                .isEqualTo(call(ExecutionListener::testEvent, testDescription, "stdout", "printing to stdout"))
        }
    }
}
