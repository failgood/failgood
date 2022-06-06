package failgood.internal

import failgood.Context
import failgood.Failure
import failgood.Pending
import failgood.Success
import failgood.TestContainer
import failgood.TestPlusResult
import failgood.internal.Colors.FAILED
import failgood.internal.Colors.PENDING
import failgood.internal.Colors.RED
import failgood.internal.Colors.RESET
import failgood.internal.Colors.SUCCESS
import failgood.internal.Colors.YELLOW
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

internal class ContextTreeReporter {
    fun stringReport(results: List<TestPlusResult>, allContexts: List<Context>): List<String> {
        val contextMap: Map<TestContainer, List<TestPlusResult>> = results.groupBy { it.test.container }
        val result = mutableListOf<String>()
        val rootContexts = allContexts.filter { it.parent == null }
        printContext(rootContexts, allContexts, result, contextMap, 0)
        return result
    }

    private fun printContext(
        contexts: List<Context>,
        allContexts: List<Context>,
        result: MutableList<String>,
        contextMap: Map<TestContainer, List<TestPlusResult>>,
        indent: Int
    ) {
        val indentString = "  ".repeat(indent)
        contexts.forEach { context ->
            result.add("$indentString* ${context.name}")
            val tests = contextMap[context]
            tests?.forEach { testResult ->
                val lines =
                    when (testResult.result) {
                        is Success -> {
                            val timeMicro = testResult.result.timeMicro
                            listOf("$indentString  $SUCCESS ${testResult.test.testName} (${time(timeMicro)}ms)")
                        }
                        is Failure -> listOf(
                            "$indentString  $FAILED ${testResult.test.testName} ${RED}FAILED$RESET",
                            "$indentString    ${
                            testResult.result.failure.message?.replace(
                                "\n",
                                "\\n"
                            )
                            }",
                            "$indentString    ${testResult.test.sourceInfo.likeStackTrace(testResult.test.testName)}"
                        )
                        is Pending -> {
                            listOf("$indentString  $PENDING ${testResult.test.testName} ${YELLOW}PENDING$RESET")
                        }
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
}
