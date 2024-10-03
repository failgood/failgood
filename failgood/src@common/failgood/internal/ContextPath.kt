package failgood.internal

import failgood.Context

/** a path to something that is contained in a context. can be a test or a context */
internal data class ContextPath(val container: Context, val name: String) : Path {
    companion object {
        fun fromList(vararg pathElements: String) =
            ContextPath(Context.fromPath(pathElements.dropLast(1)), pathElements.last())
    }

    override val path: List<String>
        get() = container.path + name

    override fun toString(): String {
        return "${container.stringPath()} > $name"
    }
}
