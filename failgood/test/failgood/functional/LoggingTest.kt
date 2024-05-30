package failgood.functional

import failgood.Test
import failgood.tests
import org.slf4j.MDC
import kotlin.test.assertNotNull


@Test
class LoggingTest {
    val tests = tests {
        describe("logging MDC") {
            it("contains the test name") {
                val mdc = assertNotNull(MDC.get("test"))
                assert(mdc.endsWith("> logging MDC > contains the test name"))
                assert(mdc.startsWith("LoggingTest"))
            }
        }
    }
}
