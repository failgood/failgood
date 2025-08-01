package failgood.internal

import failgood.Test
import failgood.testCollection

@Test
class Junit4ReporterTest {
    val tests =
        @Suppress("ktlint")
        (testCollection(Junit4Reporter::class) {
            it("reports test results") {
                val control = Junit4Reporter(TestResultFixtures.testResults).stringReport()

                assert(
                    control ==
                        listOf(
                            """<testsuite tests="3">""",
                            """<testcase classname="the test runner" name="supports describe/it syntax"/>""",
                            """<testcase classname="the test runner > contexts can be nested" name="sub-contexts also contain tests"/>""",
                            """<testcase classname="the test runner > contexts can be nested" name="failed test">""",
                            """<failure message="failure message&#13;&#10;with newline">""",
                            ExceptionPrettyPrinter(TestResultFixtures.failure)
                                .stackTrace
                                .joinToString("\n"),
                            """</failure>""",
                            """</testcase>""",
                            """</testsuite>"""))
            }
        })
}
