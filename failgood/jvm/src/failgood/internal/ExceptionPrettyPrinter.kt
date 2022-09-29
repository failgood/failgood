package failgood.internal

internal class ExceptionPrettyPrinter(private val throwable: Throwable, sourceInfo: SourceInfo? = null) {
    val stackTrace = run {
        val onlyElementsWithLineNumber = throwable.stackTrace
            .filter { it.lineNumber > 0 }

        val onlyFromFailGoodUp = onlyElementsWithLineNumber.dropLastWhile { !it.className.startsWith("failgood") }
        val onlyInTest = if (throwable !is AssertionError) onlyFromFailGoodUp else
            onlyFromFailGoodUp.filter { sourceInfo == null || it.className.contains(sourceInfo.className) }
        onlyInTest.ifEmpty { onlyFromFailGoodUp.ifEmpty { onlyElementsWithLineNumber } }
    }

    fun prettyPrint(): String {
        val cause = (throwable.cause?.let { "\ncause: $it\n${it.stackTraceToString()}" }) ?: ""
        val throwableName = if (throwable is AssertionError) "" else "${throwable::class.qualifiedName} : "
        return "${throwableName}${throwable.message}\n\tat " +
            stackTrace.joinToString("\n\tat ") + cause
    }
}
