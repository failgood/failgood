package failgood.util

import failgood.Test
import failgood.tests
import kotlinx.coroutines.delay
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Test
class RetryForTests {
    val tests = tests {
        it("rethrows exception when time is up") {
            val exception =
                assertNotNull(
                    kotlin
                        .runCatching {
                            retryFor(1.milliseconds) { throw RuntimeException("error!") }
                        }
                        .exceptionOrNull()
                )
            assert(exception.message == "error!")
        }
        it("retries for given time and returns result when it eventually succeeds") {
            var calls = 2
            // setting the retry timeout really high to avoid random test failures under load
            // TODO: decrease this again after improving test throttling.
            val result =
                retryFor(60.seconds) {
                    if (calls-- > 0) throw RuntimeException("error!")
                    "result"
                }
            assert(result == "result")
        }
        // I'm not really sure if that's really what we want, does it really have to retry at least once?
        it("retries at least once") {
            var calls = 1
            val result =
                retryFor(0.milliseconds) {
                    if (calls-- > 0) throw RuntimeException("error!")
                    "result"
                }
            assert(result == "result")
        }
        it("can call suspend functions") { retryFor { delay(0) } }
    }
}
