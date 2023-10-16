package failgood.examples

import failgood.Ignored
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Example for a custom project specific ignore: Ignore a test until a specific date. Useful when
 * you want to make sure that you don't forget about a pending test Be careful: this will break your
 * build on that date, so don't use this feature if that is a problem for you.
 */
class Until(private val dateString: String) : Ignored {

    override fun isIgnored(): String? {
        val date = LocalDate.parse(dateString).atStartOfDay().toInstant(ZoneOffset.UTC)
        val now = Instant.now()
        return if (date.isAfter(now)) "$dateString is after now" else null
    }
}
