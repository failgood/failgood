package failgood.functional

import failgood.Test
import failgood.tests
import io.github.oshai.kotlinlogging.KotlinLogging

@Test
class LoggingTest {
    private val logger = KotlinLogging.logger {}

    val tests = tests {
        describe("logging MDC") {
            it("contains the test") {
                logger.info { "Hello World!" }
            }
        }
    }
}
