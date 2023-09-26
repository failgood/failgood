package failgood

/**
 * Indicate that a test or context should be ignored. You can add your own matchers like
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
}
