package failfast

class ContextTreeReporter(results: List<TestResult>, private val allContexts: List<Context>) {
    private val contextMap = results.groupBy { it.test.parentContext }
    fun stringReport(): List<String> {
        val result = mutableListOf<String>()
        val rootContexts = allContexts.filter { it.parent == null }
        printContext(rootContexts, result, contextMap, 0)
        return result

    }

    private fun printContext(
        contexts: List<Context>,
        result: MutableList<String>,
        contextMap: Map<Context, List<TestResult>>,
        indent: Int
    ) {
        val indentString = " ".repeat(indent)
        contexts.forEach { context ->
            result.add("$indentString* ${context.name}")
            val tests = contextMap[context]
            tests?.forEach { testResult ->
                result.add("$indentString - ${testResult.test.testName}")
            }
            val childContests = allContexts.filter { it.parent == context }
            if (childContests.isNotEmpty())
                printContext(childContests, result, contextMap, indent + 1)
        }
    }

}
