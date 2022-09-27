package failgood

import java.time.Instant

/**
 * decide if a test should be ignored. you can add your own matchers like
 * ```
 * val onCi = IsIgnored { System.getenv("CI") != null }
 * ```
 */
fun interface IsIgnored {
    fun isIgnored(): Boolean

    object Always : IsIgnored {
        override fun isIgnored(): Boolean = true
    }

    object Never : IsIgnored {
        override fun isIgnored(): Boolean = false
    }

    class Until(private val instant: Instant) : IsIgnored {
        override fun isIgnored(): Boolean {
            return instant.isAfter(Instant.now())
        }
    }
}
