package failgood.util

import failgood.Test
import failgood.describe
import kotlinx.coroutines.delay
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Test
class RetryForTests {
    val tests = describe("retryFor") {
        it("rethrows exception when time is up") {
            val exception = assertNotNull(
                kotlin.runCatching {
                    retryFor(1.milliseconds) {
                        throw RuntimeException("error!")
                    }
                }.exceptionOrNull()
            )
            assert(exception.message == "error!")
        }
        it("retries for given time and returns result when it eventually succeeds") {
            var calls = 2
            // this randomly fails on windows CI when set to 1 second
            val result = retryFor(5.seconds) {
                if (calls-- > 0)
                    throw RuntimeException("error!")
                "result"
            }
            assert(result == "result")
        }
        it("retries at least once") {
            var calls = 1
            val result = retryFor(0.milliseconds) {
                if (calls-- > 0)
                    throw RuntimeException("error!")
                "result"
            }
            assert(result == "result")
        }
        it("can call suspend functions") {
            retryFor {
                delay(0)
            }
        }
    }
}
