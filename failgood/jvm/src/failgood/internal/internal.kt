package failgood.internal

import failgood.*
import failgood.findCallerSTE

internal sealed interface LoadResult {
    val order: Int
}

internal data class CouldNotLoadContext(val reason: Throwable, val jClass: Class<out Any>) : LoadResult {
    override val order: Int
        get() = 0
}

data class RootContext(
    val name: String = "root",
    val ignored: Ignored? = null,
    override val order: Int = 0,
    val isolation: Boolean = true,
    val sourceInfo: SourceInfo = SourceInfo(findCallerSTE()),
    val function: ContextLambda
) : LoadResult, Path {
    override val path: List<String>
        get() = listOf(name)
}

data class SourceInfo(val className: String, val fileName: String?, val lineNumber: Int) {
    fun likeStackTrace(testName: String) = "$className.${testName.replace(" ", "-")}($fileName:$lineNumber)"

    constructor(ste: StackTraceElement) : this(ste.className, ste.fileName!!, ste.lineNumber)
}
