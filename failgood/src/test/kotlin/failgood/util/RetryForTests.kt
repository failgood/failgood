package failgood.util

import failgood.Test
import failgood.describe
import kotlinx.coroutines.delay
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@Test
class RetryForTests {
    @OptIn(ExperimentalTime::class)
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
        it("returns result") {
            var calls = 1
            val result = retryFor(100.milliseconds) {
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
