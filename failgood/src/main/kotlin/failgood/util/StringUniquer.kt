package failgood.util

class StringUniquer {
    private val used = mutableSetOf<String>()
    fun makeUnique(path: String): String {
        return if (used.add(path))
            path
        else {
            (1..9).asSequence()
                .map { "$path-$it" }
                .first { used.add(it) }

        }
    }
}
