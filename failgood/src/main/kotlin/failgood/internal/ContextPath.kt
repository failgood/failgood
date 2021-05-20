package failgood.internal

import failgood.Context

/**
 * a path to something that is contained in a context. can be a test or a context
 */
internal data class ContextPath(val parentContext: Context, val name: String) {
    companion object {
        fun fromString(path: String): ContextPath {
            val pathElements = path.split(">").map { it.trim() }
            return ContextPath(Context.fromPath(pathElements.dropLast(1)), pathElements.last())
        }
    }

    override fun toString(): String {
        return "${parentContext.stringPath()} > $name"
    }
}
