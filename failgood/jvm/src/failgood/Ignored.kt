package failgood

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * decide if a test should be ignored. you can add your own matchers like
 * ```
 * val onCi = IsIgnored { System.getenv("CI") != null }
 * ```
 */
fun interface Ignored {
    fun isIgnored(): Boolean

    object Always : Ignored {
        override fun isIgnored(): Boolean = true
    }

    object Never : Ignored {
        override fun isIgnored(): Boolean = false
    }

    class Until(private val instant: Instant) : Ignored {
        constructor(dateString: String) : this(LocalDate.parse(dateString).atStartOfDay().toInstant(ZoneOffset.UTC))

        override fun isIgnored(): Boolean {
            return instant.isAfter(Instant.now())
        }
    }
}
