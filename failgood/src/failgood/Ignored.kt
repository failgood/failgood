package failgood

/**
 * Indicate that a test or context should be ignored. You can add your own matchers like
 *
 * ```
 * val onCi = Ignored { if(System.getenv("CI") != null) "ignored on CI" else null }
 * ```
 */
fun interface Ignored {
    // todo before the pr can be merged.
    object TODO : Ignored {
        override fun isIgnored(): String = "TODO"
    }

    fun isIgnored(): String?

    /**
     * Indicate that a test is ignored. Always give a reason
     *
     * bad:
     * ```
     * it("can fly", ignored=Because("flaky"))
     * ```
     *
     * good:
     * ```
     * it("can fly", ignored=Because("this test is flaky because it depends on shared state. see #127"))
     * ```
     */
    class Because(private val reason: String) : Ignored {
        override fun isIgnored(): String = reason
    }
    /**
     * Ignore a test unless an environment var is set.
     *
     * example
     *
     * ```
     * it("has the newest feature", ignored=UnlessEnv("NEXT"))
     * ```
     *
     * then for local development you run the tests with NEXT=1 to run your unfinished test, and on
     * others systems and in ci it is ignored.
     */
    class UnlessEnv(private val envVar: String) : Ignored {
        override fun isIgnored(): String? {
            return if (System.getenv(envVar) == null) "Ignored because env var $envVar is not set"
            else null
        }
    }
}
