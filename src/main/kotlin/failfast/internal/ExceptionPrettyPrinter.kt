package failfast.internal

class ExceptionPrettyPrinter {
    fun prettyPrint(assertionError: AssertionError): String {
        val stackTrace =
            assertionError.stackTrace.filter { it.lineNumber > 0 }
                .dropLastWhile { !it.className.startsWith("failfast") }
        return "${assertionError.message} ${stackTrace.joinToString("\t\n")}"
    }
}
