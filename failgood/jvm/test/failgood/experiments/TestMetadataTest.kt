package failgood.experiments

import failgood.Test
import failgood.TestExecutionContext
import failgood.describe
import kotlin.test.assertNotNull

@Test
object TestMetadataTest {
    val tests = describe("accessing the test metadata") {
        it("is possible for the test") {
            this.log("log body")
            assert(
                assertNotNull(this.testInfo.context.events.singleOrNull()) == TestExecutionContext.Event(
                    "stdout",
                    "log body"
                )
            )
        }
    }
}
