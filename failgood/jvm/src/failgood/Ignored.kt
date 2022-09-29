package failgood

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * decide if a test should be ignored. you can add your own matchers like
 * ```
 * val onCi = Ignored { if(System.getenv("CI") != null) "ignored on CI" else null }
 * ```
 */
fun interface Ignored {
    fun isIgnored(): String?

    /**
     * Indicate that a test is ignored. Always give a reason
     *
     * bad:
     * ```
     * it("can fly", ignored=Because("flaky"))
     * ```
     * good:
     * ```
     * it("can fly", ignored=Because("this test is flaky because it depends on shared state. see #127"))
     * ```
     *
     */
    class Because(private val reason: String) : Ignored {
        override fun isIgnored() = reason
    }

    /**
     * Ignore a test until a specific date. Useful when you want to make sure that you don't forget about a pending test
     * Be careful: this will break your build on that date, so don't use this feature if that is a problem for you.
     */
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
