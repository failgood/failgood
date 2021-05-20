package failgood.internal

import failgood.Success
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.describe
import failgood.internal.Colors.FAILED
import failgood.internal.Colors.RED
import failgood.internal.Colors.RESET
import failgood.internal.Colors.SUCCESS
import failgood.internal.TestResultFixtures.rootContext
import failgood.internal.TestResultFixtures.subContext
import failgood.internal.TestResultFixtures.subSubContext
import failgood.internal.TestResultFixtures.testResults
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.containsExactly

val stackTraceElement = StackTraceElement("class", "method", "file", 123)

@Testable
class ContextTreeReporterTest {
    val context =
        describe(ContextTreeReporter::class) {
            val reporter = ContextTreeReporter()
            it("outputs test results in tree form") {
                expectThat(
                    reporter.stringReport(testResults, listOf(rootContext, subContext))
                )
                    .containsExactly(
                        listOf(
                            "* the test runner",
                            "  $SUCCESS supports describe/it syntax (0.01ms)",
                            "  * contexts can be nested",
                            "    $SUCCESS sub-contexts also contain tests (0.02ms)",
                            "    $FAILED failed test ${RED}FAILED${RESET}",
                            "      failure message\\nwith newline",
                            "      ClassName.method(file:123)"
                        )
                    )
            }
            it("outputs empty root context") {
                expectThat(
                    reporter.stringReport(
                        listOf(
                            TestPlusResult(
                                TestDescription(
                                    subContext,
                                    "sub-contexts also contain tests", stackTraceElement
                                ),
                                Success(
                                    10
                                )
                            )
                        ), listOf(rootContext, subContext)
                    )
                )
                    .containsExactly(
                        listOf(
                            "* the test runner",
                            "  * contexts can be nested",
                            "    $SUCCESS sub-contexts also contain tests (0.01ms)"
                        )
                    )
            }
            it("outputs empty context") {
                expectThat(
                    reporter.stringReport(
                        listOf(
                            TestPlusResult(
                                TestDescription(subSubContext, "sub-contexts also contain tests", stackTraceElement),
                                Success(
                                    10
                                )
                            )
                        ),
                        listOf(rootContext, subContext, subSubContext)
                    )
                )
                    .containsExactly(
                        listOf(
                            "* the test runner",
                            "  * contexts can be nested",
                            "    * deeper",
                            "      $SUCCESS sub-contexts also contain tests (0.01ms)"
                        )
                    )
            }
            it("outputs time") {
                expectThat(
                    reporter.stringReport(
                        listOf(
                            TestPlusResult(
                                TestDescription(rootContext, "test", stackTraceElement),
                                Success(10)
                            ),
                            TestPlusResult(
                                TestDescription(rootContext, "slow test", stackTraceElement),
                                Success(1010001)
                            )
                        ),
                        listOf(rootContext)
                    )
                )
                    .containsExactly(
                        listOf(
                            "* the test runner",
                            "  $SUCCESS test (0.01ms)",
                            "  $SUCCESS slow test (1,010.0ms)"
                        )
                    )
            }
        }
}
