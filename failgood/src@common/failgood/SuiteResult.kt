package failgood

import failgood.internal.FailedTestCollectionExecution

data class SuiteResult(
    val allTests: List<TestPlusResult>,
    val failedTests: List<TestPlusResult>,
    val contexts: List<Context>,
    val failedRootContexts: List<FailedTestCollectionExecution>
) {
    val allOk: Boolean = failedTests.isEmpty() && failedRootContexts.isEmpty()

    fun check(throwException: Boolean = false, writeReport: Boolean = false) {
        checkPlatform(throwException, writeReport)
    }

    fun printSummary(printSlowest: Boolean, printPending: Boolean): Boolean {
        return printSummaryPlatform(printSlowest, printPending)
    }

    fun printSlowestTests() {
        printSlowestTestsPlatform()
    }
}

internal expect fun SuiteResult.checkPlatform(throwException: Boolean, writeReport: Boolean)

internal expect fun SuiteResult.printSummaryPlatform(
    printSlowest: Boolean,
    printPending: Boolean
): Boolean

internal expect fun SuiteResult.printSlowestTestsPlatform()
