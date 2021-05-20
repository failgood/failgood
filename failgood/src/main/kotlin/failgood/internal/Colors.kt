package failgood.internal

object Colors {
    private val isWindows = System.getProperty("os.name").startsWith("Windows")

    @Suppress("MemberVisibilityCanBePrivate")
    internal const val GREEN = "\u001B[32m"
    internal const val RED = "\u001B[31m"
    internal const val YELLOW = "\u001B[33m"
    internal const val RESET = "\u001B[0m"
    internal val SUCCESS = GREEN + (if (isWindows) "√" else "✔") + RESET
    internal val FAILED = RED + (if (isWindows) "X" else "✘") + RESET
    internal const val PENDING = "$YELLOW-$RESET"

}
