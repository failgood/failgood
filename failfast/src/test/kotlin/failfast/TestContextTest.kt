package failfast

import failfast.mock.call
import failfast.mock.getCalls
import failfast.mock.mock
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.single

object TestContextTest {
    val context = describe(TestDSL::class) {
        it("publishes a test event for stdout printing") {
            val testDescription = TestDescription(Context("root"), "testname", StackTraceElement("a", "b", "c", 1))
            val listener = mock<ExecutionListener>()
            TestContext(listener, testDescription).println("printing to stdout")
            expectThat(getCalls(listener)).single()
                .isEqualTo(call(ExecutionListener::testEvent, testDescription, "stdout", "printing to stdout"))
        }
    }
}
