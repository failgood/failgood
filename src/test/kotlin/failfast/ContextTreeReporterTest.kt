package failfast

import strikt.api.expectThat
import strikt.assertions.containsExactly

object ContextTreeReporterTest {
    val context = describe(ContextTreeReporter::class) {
        it("outputs test results in tree form") {
            val rootContext = Context("the test runner", null)
            val contextsCanBeNestedContext = Context("contexts can be nested", rootContext)
            val reporter = ContextTreeReporter(
                listOf(
                    Success(TestDescriptor(rootContext, "supports describe/it syntax")),
                    Success(TestDescriptor(contextsCanBeNestedContext, "subcontexts also contain tests"))
                )
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
    }
}

