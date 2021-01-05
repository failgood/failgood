package failfast

import failfast.internal.ContextTreeReporter
import strikt.api.expectThat
import strikt.assertions.containsExactly

object ContextTreeReporterTest {
    val context =
        describe(ContextTreeReporter::class) {
            val rootContext = Context("the test runner", null)
            val subContext = Context("contexts can be nested", rootContext)
            val subSubContext = Context("deeper", subContext)
            it("outputs test results in tree form") {
                val reporter =
                    ContextTreeReporter(
                        listOf(
                            Success(TestDescriptor(rootContext, "supports describe/it syntax"), 10),
                            Success(
                                TestDescriptor(subContext, "sub-contexts also contain tests"),
                                20
                            )
                        ),
                        listOf(rootContext, subContext)
                    )
                expectThat(reporter.stringReport())
                    .containsExactly(
                        listOf(
                            "* the test runner",
                            " - supports describe/it syntax (0.01ms)",
                            " * contexts can be nested",
                            "  - sub-contexts also contain tests (0.02ms)"
                        )
                    )
            }
            it("outputs empty root context") {
                val reporter =
                    ContextTreeReporter(
                        listOf(
                            Success(
                                TestDescriptor(subContext, "sub-contexts also contain tests"),
                                10
                            )
                        ),
                        listOf(rootContext, subContext)
                    )
                expectThat(reporter.stringReport())
                    .containsExactly(
                        listOf(
                            "* the test runner",
                            " * contexts can be nested",
                            "  - sub-contexts also contain tests (0.01ms)"
                        )
                    )
            }
            it("outputs empty context") {
                val reporter =
                    ContextTreeReporter(
                        listOf(
                            Success(
                                TestDescriptor(subSubContext, "sub-contexts also contain tests"),
                                10
                            )
                        ),
                        listOf(rootContext, subContext, subSubContext)
                    )
                expectThat(reporter.stringReport())
                    .containsExactly(
                        listOf(
                            "* the test runner",
                            " * contexts can be nested",
                            "  * deeper",
                            "   - sub-contexts also contain tests (0.01ms)"
                        )
                    )
            }
            it("outputs time") {
                val reporter =
                    ContextTreeReporter(
                        listOf(
                            Success(TestDescriptor(rootContext, "test"), 10),
                            Success(TestDescriptor(rootContext, "slow test"), 1010001)
                        ),
                        listOf(rootContext)
                    )
                expectThat(reporter.stringReport())
                    .containsExactly(
                        listOf("* the test runner", " - test (0.01ms)", " - slow test (1,010.0ms)")
                    )
            }
        }
}
