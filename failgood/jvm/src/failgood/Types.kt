package failgood

import failgood.dsl.ContextLambda
import failgood.internal.ContextPath
import kotlin.reflect.KClass

data class TestDescription(
    val container: Context,
    val testName: String,
    val sourceInfo: SourceInfo
) {
    internal constructor(
        testPath: ContextPath,
        sourceInfo: SourceInfo
    ) : this(testPath.container, testPath.name, sourceInfo)

    override fun toString(): String {
        return "${container.stringPath()} > $testName"
    }
}

internal sealed interface LoadResult {
    val order: Int
}

internal data class CouldNotLoadContext(val reason: Throwable, val kClass: KClass<out Any>) :
    LoadResult {
    override val order: Int
        get() = 0
}

fun RootContext(
    name: String = "root",
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    sourceInfo: SourceInfo = callerSourceInfo(),
    addClassName: Boolean = false,
    function: ContextLambda
) =
    RootContext(
        Context(name, null, sourceInfo, isolation),
        order,
        ignored,
        addClassName,
        function = function
    )

data class RootContext(
    val context: Context,
    override val order: Int = 0,
    val ignored: Ignored?,
    val addClassName: Boolean = false,
    val function: ContextLambda
) : LoadResult, failgood.internal.Path {
    val sourceInfo: SourceInfo
        get() =
            context.sourceInfo!! // in the root context we are sure that we always have a sourceInfo

    override val path: List<String>
        get() = listOf(context.name)
}

/** something that contains tests */
interface TestContainer {
    val parents: List<TestContainer>
    val name: String

    fun stringPath(): String
}

data class Context(
    override val name: String,
    val parent: Context? = null,
    val sourceInfo: SourceInfo? = null,
    val isolation: Boolean = true
) : TestContainer {
    companion object {
        fun fromPath(path: List<String>): Context {
            return Context(path.last(), if (path.size == 1) null else fromPath(path.dropLast(1)))
        }
    }

    override val parents: List<TestContainer> = parent?.parents?.plus(parent) ?: listOf()
    val path: List<String> = parent?.path?.plus(name) ?: listOf(name)

    override fun stringPath(): String = path.joinToString(" > ")
}
