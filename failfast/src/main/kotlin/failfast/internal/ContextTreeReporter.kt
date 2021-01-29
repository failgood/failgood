package failfast.internal

import failfast.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

internal class ContextTreeReporter {
    fun stringReport(results: List<TestResult>, allContexts: List<Context>): List<String> {
        val contextMap = results.groupBy { it.test.parentContext }
        val result = mutableListOf<String>()
        val rootContexts = allContexts.filter { it.parent == null }
        printContext(rootContexts, allContexts, result, contextMap, 0)
        return result
    }

    private fun printContext(
        contexts: List<Context>,
        allContexts: List<Context>,
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
                            listOf("$indentString  $SUCCESS ${testResult.test.testName} (${time(timeMicro)}ms)")
                        }
                        is Failed -> listOf(
                            "$indentString  $FAILED ${testResult.test.testName} ${RED}FAILED$RESET", "$indentString    ${
                                testResult.failure.message?.replace(
                                    "\n",
                                    "\\n"
                                )
                            }", "$indentString    ${testResult.stackTraceElement})"
                        )
                        is Ignored -> listOf("$indentString  $IGNORED ${testResult.test.testName} ${YELLOW}PENDING$RESET")
                    }

                result.addAll(lines)
            }
            val childContests = allContexts.filter { it.parent == context }
            if (childContests.isNotEmpty())
                printContext(childContests, allContexts, result, contextMap, indent + 1)
        }
    }

    fun time(timeMicro: Long): String = timeFormat.format(timeMicro.toDouble() / 1000)!!
    private val timeFormat = DecimalFormat("#,##0.0#", DecimalFormatSymbols(Locale.US))

    companion object {
        internal const val GREEN = "\u001B[32m"
        internal const val RED = "\u001B[31m"
        internal const val YELLOW = "\u001B[33m"
        internal const val RESET = "\u001B[0m"
        internal val SUCCESS = GREEN + (if (isWindows) "√" else "✔") + RESET
        internal val FAILED = RED + (if (isWindows) "X" else "✘") + RESET
        internal const val IGNORED = "$YELLOW-$RESET"

        private val isWindows: Boolean
            get() = System.getProperty("os.name").startsWith("Windows")
    }
}
