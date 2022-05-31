package failgood.util

import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
suspend fun <Result> retryFor(duration: Duration = 1.seconds, function: suspend () -> Result): Result {
    val until = Instant.now().plus(duration.inWholeMilliseconds, ChronoUnit.MILLIS)
    while (true) {
        try {
            return function()
        } catch (e: Throwable) {
            if (Instant.now().isAfter(until))
                throw e
            delay(10)
        }
    }
}
