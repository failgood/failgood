package failgood

import failgood.internal.Colors
import failgood.internal.ContextTreeReporter
import failgood.internal.FailedRootContext
import failgood.internal.Junit4Reporter
import failgood.internal.sysinfo.uptime
import failgood.internal.util.getenv
import failgood.internal.util.pluralize
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

data class SuiteResult(
    val allTests: List<TestPlusResult>,
    val failedTests: List<TestPlusResult>,
    val contexts: List<Context>,
    val failedRootContexts: List<FailedRootContext>
) {
    val allOk = failedTests.isEmpty() && failedRootContexts.isEmpty()

    fun check(throwException: Boolean = false, writeReport: Boolean = false) {
        // **/build/test-results/test/TEST-*.xml'
        if (writeReport) {
            val reportDir = Paths.get("build", "test-results", "test")
            Files.createDirectories(reportDir)
            Files.write(
                reportDir.resolve("TEST-failgood.xml"),
                Junit4Reporter(allTests).stringReport().joinToString("\n").encodeToByteArray()
            )
        }
        if (printSummary(getenv("PRINT_SLOWEST") != null, getenv("PRINT_PENDING") != null)) return

        if (throwException) throw SuiteFailedException("test failed")
        exitProcess(-1)
    }

    fun printSummary(printSlowest: Boolean, printPending: Boolean): Boolean {
        val totalTests = allTests.size
        if (allOk) {
            if (printSlowest) printSlowestTests()
            val pendingTests = allTests.filter { it.isSkipped }
            if (pendingTests.isNotEmpty()) {
                if (printPending) printPendingTests(pendingTests)
                val pending = pendingTests.size
                println(
                    pluralize(totalTests, "test") +
                        ". ${totalTests - pending} ok, $pending pending. time: ${
                                uptime(
                                    totalTests
                                )
                            }"
                )
                return true
            }
            println(pluralize(totalTests, "test") + ". time: ${uptime(totalTests)}")
            return true
        } else {
            val message = failedTests.joinToString(separator = "\n") { it.prettyPrint() }
            println("${Colors.RED}FAILED:${Colors.RESET}\n$message")
            println(
                "$totalTests tests. ${failedTests.size} failed. total time: ${uptime(totalTests)}"
            )
        }
        return false
    }

    private fun printPendingTests(pendingTests: List<TestPlusResult>) {
        println("\nPending tests:")
        pendingTests.forEach { println(it.test) }
    }

    fun printSlowestTests() {
        // be very gentle and consider any tests faster than 500ms as not slow
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
            println("${contextTreeReporter.time((it.result as Success).timeMicro)}ms ${it.test}")
        }
    }
}
