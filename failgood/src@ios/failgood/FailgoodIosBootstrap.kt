package failgood

/**
 * Kotlin entrypoint for failgood suites on iOS.
 *
 * Build a bootstrap in `iosTest` and call [run] or [runAndThrow] from a `kotlin.test` test. The
 * native Gradle iOS app-test tasks package that test executable and run it on a simulator or, via
 * `iosAppTest`, on a configured real device.
 */
class FailgoodIosBootstrap(val name: String, rootContexts: Collection<TestCollection<*>>) {
    private val suite = Suite(rootContexts)

    constructor(name: String, rootContext: TestCollection<*>) : this(name, listOf(rootContext))

    constructor(
        name: String,
        vararg rootContexts: TestCollection<*>
    ) : this(name, rootContexts.toList())

    fun run(silent: Boolean = true): SuiteResult = suite.run(parallelism = 1, silent = silent)

    @Throws(FailGoodException::class)
    fun runAndThrow(silent: Boolean = true) {
        val result = run(silent)
        if (result.allOk) return
        result.printSummary(printSlowest = true, printPending = false)
        throw FailGoodException("failgood iOS bootstrap \"$name\" failed")
    }
}
