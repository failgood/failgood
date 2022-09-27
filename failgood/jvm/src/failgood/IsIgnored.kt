package failgood

/**
 * decide if a test should be ignored. you can add your own matchers like
 * ```
 * val onCi = IsIgnored { System.getenv("CI") != null }
 * ```
 */
fun interface IsIgnored {
    fun isIgnored(): Boolean
}

object IgnoreAlways : IsIgnored {
    override fun isIgnored(): Boolean = true
}

object IgnoreNever : IsIgnored {
    override fun isIgnored(): Boolean = false
}
