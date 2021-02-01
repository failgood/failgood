package failfast.internal

import failfast.Success
import failfast.TestDescription
import failfast.describe
import failfast.internal.Colors.FAILED
import failfast.internal.Colors.RED
import failfast.internal.Colors.RESET
import failfast.internal.Colors.SUCCESS
import failfast.internal.TestResultFixtures.rootContext
import failfast.internal.TestResultFixtures.subContext
import failfast.internal.TestResultFixtures.subSubContext
import failfast.internal.TestResultFixtures.testResults
import strikt.api.expectThat
import strikt.assertions.containsExactly

val stackTraceElement = RuntimeException().stackTrace.first()
object ContextTreeReporterTest {
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
                            Success(
                                TestDescription(
                                    subContext,
                                    "sub-contexts also contain tests", stackTraceElement
                                ), 10
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
                            Success(
                                TestDescription(subSubContext, "sub-contexts also contain tests", stackTraceElement),
                                10
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
                            Success(TestDescription(rootContext, "test", stackTraceElement), 10),
                            Success(TestDescription(rootContext, "slow test", stackTraceElement), 1010001)
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
