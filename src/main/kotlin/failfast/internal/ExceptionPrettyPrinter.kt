package failfast.internal

class ExceptionPrettyPrinter {
    fun prettyPrint(throwable: Throwable): String {
        val stackTrace =
            throwable.stackTrace.filter { it.lineNumber > 0 }
                .dropLastWhile { !it.className.startsWith("failfast") }
        return "${throwable.javaClass.name} : ${throwable.message}\n\tat ${stackTrace.joinToString("\n\tat ")}"
    }
}
