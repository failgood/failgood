package failfast

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class ContextTreeReporter(results: List<TestResult>, private val allContexts: List<Context>) {
    val timeFormat = DecimalFormat("#,##0.0#", DecimalFormatSymbols(Locale.US))
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
                val line = when (testResult) {
                    is Success -> "$indentString - ${testResult.test.testName} (${timeFormat.format(testResult.timeMicro.toDouble() / 1000)}ms)"
                    is Failed -> "$indentString - ${testResult.test.testName}"
                    is Ignored -> "$indentString - ${testResult.test.testName}"
                }

                result.add(line)
            }
            val childContests = allContexts.filter { it.parent == context }
            if (childContests.isNotEmpty())
                printContext(childContests, result, contextMap, indent + 1)
        }
    }

}
