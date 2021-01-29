package failfast.internal

class ExceptionPrettyPrinter(private val throwable: Throwable) {
    val stackTrace = run {
        val onlyElementsWithLineNumber = throwable.stackTrace
            .filter { it.lineNumber > 0 }

        val onlyFailfast = onlyElementsWithLineNumber.dropLastWhile { !it.className.startsWith("failfast") }
        onlyFailfast.ifEmpty { onlyElementsWithLineNumber }
    }

    fun prettyPrint(): String {
        val cause = (throwable.cause?.let { "\ncause: $it\n${it.stackTraceToString()}" }) ?: ""
        return "${throwable::class.qualifiedName} : ${throwable.message}\n\tat " +
                stackTrace.joinToString("\n\tat ") + cause
    }
}
