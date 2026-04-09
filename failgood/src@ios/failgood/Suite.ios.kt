package failgood

import failgood.internal.TestFilterProvider
import failgood.internal.execution.executionContext
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

internal actual fun runSuiteBlocking(
    suite: Suite,
    parallelism: Int?,
    silent: Boolean,
    filter: TestFilterProvider,
    listener: ExecutionListener
): SuiteResult {
    return runBlocking(executionContext()) {
        if (!silent) {
            println("starting test suite with parallelism = 1")
        }
        awaitTestResults(
            suite.findAndStartTests(this, filter = filter, listener = listener).awaitAll())
    }
}
