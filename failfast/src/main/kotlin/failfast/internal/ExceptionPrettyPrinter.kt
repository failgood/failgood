package failfast.internal

class ExceptionPrettyPrinter(private val throwable: Throwable) {
    val stackTrace =
        throwable.stackTrace
            .filter { it.lineNumber > 0 }
            .dropLastWhile { !it.className.startsWith("failfast") }

    fun prettyPrint(): String {
        return "${throwable::class.qualifiedName} : ${throwable.message}\n\tat " +
                stackTrace.joinToString("\n\tat ") + throwable.cause?.let { "\ncause: $it\n${it.stackTraceToString()}" }
    }
}
