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
    fun isIgnored(): String?

    class Because(private val reason: String) : Ignored {
        override fun isIgnored() = reason
    }

    class Until(private val dateString: String) : Ignored {

        override fun isIgnored(): String? {
            val date = LocalDate.parse(dateString).atStartOfDay().toInstant(ZoneOffset.UTC)
            val now = Instant.now()
            return if (date.isAfter(now))
                "$dateString is after now"
            else
                null
        }
    }
}
