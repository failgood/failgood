package failfast.internal

import failfast.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

internal class ContextTreeReporter(results: List<TestResult>, private val allContexts: List<Context>) {
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
        val indentString = "  ".repeat(indent)
        contexts.forEach { context ->
            result.add("$indentString* ${context.name}")
            val tests = contextMap[context]
            tests?.forEach { testResult ->
                val lines =
                    when (testResult) {
                        is Success -> {
                            val timeMicro = testResult.timeMicro
                            listOf("$indentString  - ${testResult.test.testName} (${time(timeMicro)}ms)")
                        }
                        is Failed -> listOf(
                            "$indentString  - ${testResult.test.testName} FAILED", "$indentString    ${
                                testResult.failure.message?.replace(
                                    "\n",
                                    "\\n"
                                )
                            }", "$indentString    ${testResult.stackTraceElement})"
                        )
                        is Ignored -> listOf("$indentString- ${testResult.test.testName} PENDING")
                    }

                result.addAll(lines)
            }
            val childContests = allContexts.filter { it.parent == context }
            if (childContests.isNotEmpty())
                printContext(childContests, result, contextMap, indent + 1)
        }
    }

    companion object {
        fun time(timeMicro: Long): String = timeFormat.format(timeMicro.toDouble() / 1000)!!
        private val timeFormat = DecimalFormat("#,##0.0#", DecimalFormatSymbols(Locale.US))
    }
}
