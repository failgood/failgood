package failgood.internal.util

internal class StringUniquer {
    private val used = mutableSetOf<String>()

    /** this adds a number to a string to make it unique within this instance */
    fun makeUnique(string: String): String {
        return if (used.add(string)) string
        else {
            (1..Int.MAX_VALUE).asSequence().map { "$string-$it" }.first { used.add(it) }
        }
    }
}
