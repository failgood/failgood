package failgood.internal

import failgood.describe
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.containsExactly


@Testable
class Junit4ReporterTest {
    val context = describe(Junit4Reporter::class) {
        it("reports test results") {
            val control = Junit4Reporter(TestResultFixtures.testResults).stringReport()

            expectThat(control).containsExactly(
                listOf(
                    """<testsuite tests="3">""",
                    """<testcase classname="the test runner" name="supports describe/it syntax"/>""",
                    """<testcase classname="the test runner > contexts can be nested" name="sub-contexts also contain tests"/>""",
                    """<testcase classname="the test runner > contexts can be nested" name="failed test">""",
                    """<failure message="failure message&#13;&#10;with newline">""",
                    ExceptionPrettyPrinter(TestResultFixtures.failure).stackTrace.joinToString("\n"),
                    """</failure>""",
                    """</testcase>""",
                    """</testsuite>"""
                )
            )
        }
    }
}

