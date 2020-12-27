package failfast

import strikt.api.expectThat
import strikt.assertions.containsExactly

object ContextTreeReporterTest {
    val context = describe(ContextTreeReporter::class) {
        val rootContext = Context("the test runner", null)
        val subContext = Context("contexts can be nested", rootContext)
        val subSubContext = Context("deeper", subContext)
        it("outputs test results in tree form") {
            val reporter = ContextTreeReporter(
                listOf(
                    Success(TestDescriptor(rootContext, "supports describe/it syntax")),
                    Success(TestDescriptor(subContext, "subcontexts also contain tests"))
                ), listOf(rootContext, subContext)
            )
            expectThat(reporter.stringReport()).containsExactly(
                listOf(
                    "* the test runner",
                    " - supports describe/it syntax",
                    " * contexts can be nested",
                    "  - subcontexts also contain tests"
                )
            )
        }
        it("outputs empty root context") {
            val reporter = ContextTreeReporter(
                listOf(
                    Success(TestDescriptor(subContext, "subcontexts also contain tests"))
                ), listOf(rootContext, subContext)
            )
            expectThat(reporter.stringReport()).containsExactly(
                listOf(
                    "* the test runner",
                    " * contexts can be nested",
                    "  - subcontexts also contain tests"
                )
            )
        }
        it("outputs empty context") {
            val reporter = ContextTreeReporter(
                listOf(
                    Success(TestDescriptor(subSubContext, "subcontexts also contain tests"))
                ), listOf(rootContext, subContext, subSubContext)
            )
            expectThat(reporter.stringReport()).containsExactly(
                listOf(
                    "* the test runner",
                    " * contexts can be nested",
                    "  * deeper",
                    "   - subcontexts also contain tests"
                )
            )
        }
    }
}

