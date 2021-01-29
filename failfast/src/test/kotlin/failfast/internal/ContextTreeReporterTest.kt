package failfast.internal

import failfast.Success
import failfast.TestDescriptor
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
                            "      failfast.internal.ContextTreeReporterTest.invokeSuspend(ContextTreeReporterTest.kt:16)"
                        )
                    )
            }
            it("outputs empty root context") {
                expectThat(
                    reporter.stringReport(
                        listOf(
                            Success(
                                TestDescriptor(
                                    subContext,
                                    "sub-contexts also contain tests"
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
                                TestDescriptor(subSubContext, "sub-contexts also contain tests"),
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
                            "      $SUCCESS ub-contexts also contain tests (0.01ms)"
                        )
                    )
            }
            it("outputs time") {
                expectThat(
                    reporter.stringReport(
                        listOf(
                            Success(TestDescriptor(rootContext, "test"), 10),
                            Success(TestDescriptor(rootContext, "slow test"), 1010001)
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
