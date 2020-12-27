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

class ContextTreeReporter(private val results: List<TestResult>) {
    fun stringReport(): List<String> {
        val result = mutableListOf<String>()
        val contextMap = results.groupBy { it.test.parentContext }
        val rootContexts = contextMap.keys.filter { it.parent == null }
        printContext(rootContexts, result, contextMap, 0)
        return result

    }

    private fun printContext(
        rootContexts: List<Context>,
        result: MutableList<String>,
        contextMap: Map<Context, List<TestResult>>,
        indent: Int
    ) {
        val indentString = " ".repeat(indent)
        rootContexts.forEach { context ->
            result.add("$indentString* ${context.name}")
            val tests = contextMap[context]
            tests!!.forEach { testResult ->
                result.add("$indentString - ${testResult.test.testName}")
            }
            val childContests = contextMap.keys.filter { it.parent == context }
            if (childContests.isNotEmpty())
                printContext(childContests, result, contextMap, indent + 1)
        }
    }

}
