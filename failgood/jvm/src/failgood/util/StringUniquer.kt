package failgood.util

internal class StringUniquer {
    private val used = mutableSetOf<String>()
    fun makeUnique(path: String): String {
        return if (used.add(path))
            path
        else {
            (1..Int.MAX_VALUE).asSequence()
                .map { "$path-$it" }
                .first { used.add(it) }
        }
    }
}
