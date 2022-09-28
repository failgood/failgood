package failgood

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * decide if a test should be ignored. you can add your own matchers like
 * ```
 * val onCi = Ignored { System.getenv("CI") != null }
 * ```
 */
fun interface Ignored {
    fun isIgnored(): IgnoreReason

    class Because(private val reason: String) : Ignored {
        override fun isIgnored() = IgnoredBecause(reason)
    }

    object Never : Ignored {
        override fun isIgnored() = NotIgnored
    }

    class Until(private val dateString: String) : Ignored {

        override fun isIgnored(): IgnoreReason {
            val date = LocalDate.parse(dateString).atStartOfDay().toInstant(ZoneOffset.UTC)
            val now = Instant.now()
            return if (date.isAfter(now))
                IgnoredBecause("$dateString is after now")
            else
                NotIgnored
        }
    }
}

sealed interface IgnoreReason
object NotIgnored : IgnoreReason
data class IgnoredBecause(val reason: String) : IgnoreReason
