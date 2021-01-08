package failfast.internal

import failfast.Failed
import failfast.Ignored
import failfast.Success
import failfast.TestResult
import failfast.describe
import strikt.api.expectThat
import strikt.assertions.containsExactly


object Junit4ReporterTest {
    val context = describe(Junit4Reporter::class) {
        itWill("report test results") {
            val control = Junit4Reporter(TestResultFixtures.testResults).stringReport()

            expectThat(control).containsExactly(
                listOf(
                    """<testsuite tests="3">""",
                    """<testcase classname="the test runner" name="supports describe/it syntax"/>""",
                    """<testcase classname="the test runner > contexts can be nested" name="sub-contexts also contain tests"/>""",
                    """<testcase classname="the test runner > contexts can be nested" name="failed test">""",
                    """<failure message="this is the message">""",
                    """</testsuite>"""
                )
            )
        }
    }
}

class Junit4Reporter(private val testResults: List<TestResult>) {
    fun stringReport(): List<String> {
        val result = mutableListOf("<testsuite tests=\"${testResults.size}\">")
        testResults.forEach {

            val line = when (it) {
                is Success ->
                    listOf("""<testcase classname="${it.test.parentContext.stringPath()}" name="${it.test.testName}"/>""")
                is Failed -> {
                    listOf(
                        """<testcase classname="${it.test.parentContext.stringPath()}" name="${it.test.testName}">""",
                        """<failure message="${it.failure.message}">""",
                        it.failure.stackTraceToString(),
                        """</failure>""",
                        """</testcase>"""
                    )
                }
                is Ignored -> {
                    listOf(
                        """<testcase classname="${it.test.parentContext.stringPath()}" name="${it.test.testName}">""",
                        """<skipped/></testcase>"""
                    )
                }
            }
            result.addAll(line)
        }
        result.add("</testsuite>")
        return result
    }

}
