package failgood

import failgood.internal.Colors
import failgood.internal.ContextTreeReporter
import failgood.internal.FailedRootContext
import failgood.internal.Junit4Reporter
import failgood.util.getenv
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

    @Suppress("UNREACHABLE_CODE")
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
        val totalTests = allTests.size
        if (allOk) {
            if (getenv("PRINT_SLOWEST") != null)
                printSlowestTests()
            val pendingTests = allTests.filter { it.isSkipped }
            if (pendingTests.isNotEmpty()) {
                // printPendingTests(ignoredTests)
                val pending = pendingTests.size
                println(
                    pluralize(totalTests, "test") + ". ${totalTests - pending} ok, $pending pending. time: ${
                    uptime(
                        totalTests
                    )
                    }"
                )
                return
            }
            println(pluralize(totalTests, "test") + ". time: ${uptime(totalTests)}")
            return
        }
        if (throwException) throw SuiteFailedException("test failed") else {
            val message =
                failedTests.joinToString(separator = "\n") {
                    it.prettyPrint()
                }
            @Suppress("unused")
            println("${Colors.RED}FAILED:${Colors.RESET}\n$message")
            println("$totalTests tests. ${failedTests.size} failed. total time: ${uptime(totalTests)}")
            exitProcess(-1)
        }
        @Suppress("unused")
        fun printPendingTests(pendingTests: List<TestPlusResult>) {
            println("\nPending tests:")
            pendingTests.forEach { println(it.test) }
        }
    }

    fun printSlowestTests() {
        val contextTreeReporter = ContextTreeReporter()
        val slowTests =
            allTests.filter { it.isSuccess }.sortedBy { 0 - (it.result as Success).timeMicro }.take(5)
        println("Slowest tests:")
        slowTests.forEach { println("${contextTreeReporter.time((it.result as Success).timeMicro)}ms ${it.test}") }
    }
}
