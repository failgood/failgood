
package failgood.tools

import failgood.Test
import failgood.describe
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertNotNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
@Test
class RetryForTests {
    @OptIn(ExperimentalTime::class)
    val tests = describe(::retryFor.name) {
        it("retries when time is not yet up") {
            val exception = assertNotNull(kotlin.runCatching {
                retryFor(1.milliseconds) {
                    throw RuntimeException("error!")
                }
            }.exceptionOrNull())
            assert(exception.message == "error!")
        }

    }
}

@OptIn(ExperimentalTime::class)
private suspend fun retryFor(duration: Duration = 1.seconds, function: suspend () -> Unit) {
    val until = Instant.now().plus(duration.inWholeMilliseconds, ChronoUnit.MILLIS)
    while (true) {
        try {
            return function()
        } catch (e: Throwable) {
            if (Instant.now().isAfter(until))
                throw e
            delay(100)
        }
    }

}
