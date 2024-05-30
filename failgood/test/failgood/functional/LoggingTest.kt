package failgood.functional

import failgood.Test
import failgood.tests
import org.junit.jupiter.api.Assertions.assertNull
import org.slf4j.MDC
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread
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
            it("loses the mdc when a new thread is started") {
                val cf = CompletableFuture<String>()
                thread {
                    cf.complete(MDC.get("test"))
                }.join()

                assertNull(cf.get())
            }
        }
    }
}
