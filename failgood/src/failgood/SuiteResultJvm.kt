package failgood

import failgood.internal.Colors
import failgood.internal.ContextTreeReporter
import failgood.internal.Junit4Reporter
import failgood.internal.sysinfo.uptime
import failgood.internal.util.getenv
import failgood.internal.util.pluralize
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

internal actual fun SuiteResult.checkPlatform(throwException: Boolean, writeReport: Boolean) {
    if (writeReport) {
        val reportDir = Paths.get("build", "test-results", "test")
        Files.createDirectories(reportDir)
        Files.write(
            reportDir.resolve("TEST-failgood.xml"),
            Junit4Reporter(allTests).stringReport().joinToString("\n").encodeToByteArray())
    }
    if (printSummaryPlatform(getenv("PRINT_SLOWEST") != null, getenv("PRINT_PENDING") != null)) {
        return
    }

    if (throwException) throw SuiteFailedException("test failed")
    exitProcess(-1)
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
    } else {
        val message = failedTests.joinToString(separator = "\n") { it.prettyPrint() }
        println("${Colors.RED}FAILED:${Colors.RESET}\n$message")
        println("$totalTests tests. ${failedTests.size} failed. total time: ${uptime(totalTests)}")
    }
    return false
}

internal actual fun SuiteResult.printSlowestTestsPlatform() {
    val veryLongTimeForATestToTake = 500 * 1000
    val contextTreeReporter = ContextTreeReporter()
    val slowTests =
        allTests
            .filter { it.isSuccess }
            .filter { (it.result as Success).timeMicro > veryLongTimeForATestToTake }
            .sortedBy { 0 - (it.result as Success).timeMicro }
            .take(5)
    if (slowTests.isEmpty()) return
    println("Slowest tests:")
    slowTests.forEach {
        println(
            "${contextTreeReporter.time((it.result as Success).timeMicro)}ms ${it.test.niceString()}")
    }
}

private fun SuiteResult.printPendingTests(pendingTests: List<TestPlusResult>) {
    println("\nPending tests:")
    pendingTests.forEach { println(it.test) }
}
