package failfast

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
