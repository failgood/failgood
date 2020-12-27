package failfast

class ContextTreeReporter(private val results: List<TestResult>, private val rootContexts: List<Context>) {
    fun stringReport(): List<String> {
        val result = mutableListOf<String>()
        val contextMap = results.groupBy { it.test.parentContext }
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
            val childContests = contextMap.keys.filter { it.parent == context }
            if (childContests.isNotEmpty())
                printContext(childContests, result, contextMap, indent + 1)
        }
    }

}
