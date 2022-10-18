package failgood.util

import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun <Result> retryFor(duration: Duration = 1.seconds, function: suspend () -> Result): Result {
    var calls = 0
    val until = Instant.now().plus(duration.inWholeMilliseconds, ChronoUnit.MILLIS)
    while (true) {
        try {
            return function()
        } catch (e: Throwable) {
            if (calls++ != 0)
                if (Instant.now().isAfter(until))
                    throw e
            delay(10)
        }
    }
}
