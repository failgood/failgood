package failgood

import failgood.internal.Colors
import failgood.internal.sysinfo.uptime
import failgood.internal.util.pluralize

internal actual fun SuiteResult.checkPlatform(throwException: Boolean, writeReport: Boolean) {
    if (writeReport) {
        println("writing junit reports is not supported on this platform")
    }
    if (printSummaryPlatform(printSlowest = true, printPending = false)) {
        return
    }
    if (throwException) throw SuiteFailedException("test failed")
    throw SuiteFailedException("test failed")
}

internal actual fun SuiteResult.printSummaryPlatform(
    printSlowest: Boolean,
    printPending: Boolean
): Boolean {
    val totalTests = allTests.size
    if (allOk) {
        if (printSlowest) {
            printSlowestTestsPlatform()
        }
        val pendingTests = allTests.filter { it.isSkipped }
        if (pendingTests.isNotEmpty()) {
            if (printPending) {
                printPendingTests(pendingTests)
            }
            val pending = pendingTests.size
            println(
                pluralize(totalTests, "test") +
                    ". ${totalTests - pending} ok, $pending pending. time: ${uptime(totalTests)}")
            return true
        }
        println(pluralize(totalTests, "test") + ". time: ${uptime(totalTests)}")
        return true
    }
    val message = failedTests.joinToString(separator = "\n") { it.prettyPrint() }
    println("${Colors.RED}FAILED:${Colors.RESET}\n$message")
    println("$totalTests tests. ${failedTests.size} failed. total time: ${uptime(totalTests)}")
    return false
}

internal actual fun SuiteResult.printSlowestTestsPlatform() {
    val veryLongTimeForATestToTake = 500 * 1000
    val slowTests =
        allTests
            .filter { it.isSuccess }
            .filter { (it.result as Success).timeMicro > veryLongTimeForATestToTake }
            .sortedBy { 0 - (it.result as Success).timeMicro }
            .take(5)
    if (slowTests.isEmpty()) return
    println("Slowest tests:")
    slowTests.forEach {
        println("${(it.result as Success).timeMicro / 1000.0}ms ${it.test.niceString()}")
    }
}

private fun SuiteResult.printPendingTests(pendingTests: List<TestPlusResult>) {
    println("\nPending tests:")
    pendingTests.forEach { println(it.test) }
}
