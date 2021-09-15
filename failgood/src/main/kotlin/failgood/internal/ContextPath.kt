package failgood.internal

import failgood.Context

/**
 * a path to something that is contained in a context. can be a test or a context
 */
internal data class ContextPath(val container: Context, val name: String) {
    companion object {
        fun fromString(path: String): ContextPath {
            val pathElements = path.split(">").map { it.trim() }
            return ContextPath(Context.fromPath(pathElements.dropLast(1)), pathElements.last())
        }
    }

    override fun toString(): String {
        return "${container.stringPath()} > $name"
    }

    fun startsWith(filter: List<String>?): Boolean {
        if (filter == null)
            return true

        val path = container.path + name
        if (filter.size > path.size)
            return false
        return filter == path.subList(0, filter.size)
    }
}
