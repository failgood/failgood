package failgood.util

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/** retries a [block] for a [duration], but at least once */
suspend fun <Result> retryFor(duration: Duration = 1.seconds, block: suspend () -> Result): Result {
    var calls = 0
    val until = Instant.now().plus(duration.inWholeMilliseconds, ChronoUnit.MILLIS)
    while (true) {
        try {
            return block()
        } catch (e: Throwable) {
            if (calls++ != 0) if (Instant.now().isAfter(until)) throw e
            delay(10)
        }
    }
}
