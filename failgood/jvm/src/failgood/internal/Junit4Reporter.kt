package failgood.internal

import failgood.Failure
import failgood.Pending
import failgood.Success
import failgood.TestPlusResult

// based on a snippet by Ben Woodworth on the kotlin slack
internal fun String.xmlEscape(): String = buildString(length + 30) {
    for (char in this@xmlEscape) {
        when (char) {
            '\n' -> append("&#13;&#10;")
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(char)
        }
    }
}

internal class Junit4Reporter(private val testResults: List<TestPlusResult>) {
    fun stringReport(): List<String> {
        val result = mutableListOf("<testsuite tests=\"${testResults.size}\">")
        testResults.forEach {
            val line = when (it.result) {
                is Success ->
                    listOf("""<testcase classname="${it.test.container.stringPath()}" name="${it.test.testName}"/>""")
                is Failure -> {
                    listOf(
                        """<testcase classname="${it.test.container.stringPath()}" name="${it.test.testName}">""",
                        """<failure message="${it.result.failure.message?.xmlEscape()}">""",
                        ExceptionPrettyPrinter(it.result.failure).stackTrace.joinToString("\n"),
                        """</failure>""",
                        """</testcase>"""
                    )
                }
                is Pending -> {
                    listOf(
                        """<testcase classname="${it.test.container.stringPath()}" name="${it.test.testName}">""",
                        """<skipped/></testcase>"""
                    )
                }
            }
            result.addAll(line)
        }
        result.add("</testsuite>")
        return result
    }
}
