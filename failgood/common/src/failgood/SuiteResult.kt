package failgood

import failgood.internal.FailedRootContext

data class SuiteResult(
    val allTests: List<TestPlusResult>,
    val failedTests: List<TestPlusResult>,
    val contexts: List<Context>,
    val failedRootContexts: List<FailedRootContext>
) {
    val allOk = failedTests.isEmpty() && failedRootContexts.isEmpty()
}
