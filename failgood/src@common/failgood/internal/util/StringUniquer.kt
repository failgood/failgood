package failgood.internal.util

internal class StringUniquer {
    private val used = mutableSetOf<String>()

    fun makeUnique(string: String): String {
        return if (used.add(string)) {
            string
        } else {
            (1..Int.MAX_VALUE).asSequence().map { "$string-$it" }.first { used.add(it) }
        }
    }
}
