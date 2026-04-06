package failgood

/**
 * Explicit iOS entrypoint for failgood suites.
 *
 * Build a bootstrap in Kotlin and call [runAndThrow] from an XCTest method to run the registered
 * failgood suites on a simulator or device.
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
