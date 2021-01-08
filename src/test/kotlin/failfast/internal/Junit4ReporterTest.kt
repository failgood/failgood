package failfast.internal

import failfast.TestResult
import failfast.describe
import strikt.api.expectThat
import strikt.assertions.containsExactly


object Junit4ReporterTest {
    val context = describe(Junit4Reporter::class) {
        val control = Junit4Reporter(testResults).stringReport()

        expectThat(control).containsExactly(
            listOf(
                """<testsuite tests="2">""",
                """<testcase classname="the test runner" name="supports describe/it syntax"/>""",
                """<testcase classname="the test runner > contexts can be nested" name="sub-contexts also contain tests"/>""",
                """</testsuite>"""
            )
        )
    }
}

class Junit4Reporter(private val testResults: List<TestResult>) {
    fun stringReport(): List<String> {
        val result = mutableListOf("<testsuite tests=\"${testResults.size}\">")
        testResults.forEach {
            result.add("""<testcase classname="${it.test.parentContext.stringPath()}" name="${it.test.testName}"/>""")
        }
        result.add("</testsuite>")
        return result
    }

}
