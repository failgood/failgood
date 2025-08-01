package failgood.internal

import failgood.SourceInfo
import failgood.Success
import failgood.Test
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.internal.Colors.FAILED
import failgood.internal.Colors.RED
import failgood.internal.Colors.RESET
import failgood.internal.Colors.SUCCESS
import failgood.internal.TestResultFixtures.rootContext
import failgood.internal.TestResultFixtures.subContext
import failgood.internal.TestResultFixtures.subSubContext
import failgood.internal.TestResultFixtures.testResults
import failgood.testCollection

val sourceInfo = SourceInfo("class", "file", 123)

@Test
class ContextTreeReporterTest {
    val tests =
        testCollection(ContextTreeReporter::class) {
            val reporter = ContextTreeReporter()
            it("outputs test results in tree form") {
                val result =
                    reporter
                        .stringReport(testResults, listOf(rootContext, subContext))
                        .joinToString("\n")
                val expected =
                    listOf(
                            "* the test runner",
                            "  $SUCCESS supports describe/it syntax (0.01ms)",
                            "  * contexts can be nested",
                            "    $SUCCESS sub-contexts also contain tests (0.02ms)",
                            "    $FAILED failed test ${RED}FAILED$RESET",
                            "      failure message\\nwith newline",
                            "      ClassName.failed-test(file:123)")
                        .joinToString("\n")
                assert(result == expected)
            }
            it("outputs empty root context") {
                val result =
                    reporter.stringReport(
                        listOf(
                            TestPlusResult(
                                TestDescription(
                                    subContext, "sub-contexts also contain tests", sourceInfo),
                                Success(10))),
                        listOf(rootContext, subContext))
                assert(
                    result ==
                        listOf(
                            "* the test runner",
                            "  * contexts can be nested",
                            "    $SUCCESS sub-contexts also contain tests (0.01ms)"))
            }
            it("outputs empty context") {
                val result =
                    reporter.stringReport(
                        listOf(
                            TestPlusResult(
                                TestDescription(
                                    subSubContext, "sub-contexts also contain tests", sourceInfo),
                                Success(10))),
                        listOf(rootContext, subContext, subSubContext))
                assert(
                    result ==
                        listOf(
                            "* the test runner",
                            "  * contexts can be nested",
                            "    * deeper",
                            "      $SUCCESS sub-contexts also contain tests (0.01ms)"))
            }
            it("outputs time") {
                val result =
                    reporter.stringReport(
                        listOf(
                            TestPlusResult(
                                TestDescription(rootContext, "test", sourceInfo), Success(10)),
                            TestPlusResult(
                                TestDescription(rootContext, "slow test", sourceInfo),
                                Success(1010001))),
                        listOf(rootContext))
                assert(
                    result ==
                        listOf(
                            "* the test runner",
                            "  $SUCCESS test (0.01ms)",
                            "  $SUCCESS slow test (1,010.0ms)"))
            }
        }
}
