package failgood

import failgood.internal.ContextPath
import failgood.internal.util.niceString
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

fun describe(
    subjectDescription: String,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextLambda
): RootContext = RootContext(subjectDescription, ignored, order, isolation, function = function)

inline fun <reified T> describe(
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    noinline function: ContextLambda
): RootContext = describe(typeOf<T>(), ignored, order, isolation, function)

fun describe(
    subjectType: KType,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextLambda
): RootContext = RootContext(subjectType.niceString(), ignored, order, isolation, function = function)

fun describe(
    subjectType: KClass<*>,
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextLambda
): RootContext = RootContext("${subjectType.simpleName}", ignored, order, isolation, function = function)

suspend inline fun <reified Class> ContextDSL<*>.describe(
    tags: Set<String> = setOf(),
    isolation: Boolean? = null,
    ignored: Ignored? = null,
    noinline contextLambda: ContextLambda
) = this.describe(Class::class.simpleName!!, tags, isolation, ignored, contextLambda)

data class TestDescription(
    val container: Context,
    val testName: String,
    val sourceInfo: SourceInfo
) {
    internal constructor(testPath: ContextPath, sourceInfo: SourceInfo) : this(
        testPath.container, testPath.name, sourceInfo
    )

    override fun toString(): String {
        return "${container.stringPath()} > $testName"
    }
}

internal sealed interface LoadResult {
    val order: Int
}

internal data class CouldNotLoadContext(val reason: Throwable, val jClass: KClass<out Any>) : LoadResult {
    override val order: Int
        get() = 0
}

fun RootContext(
    name: String = "root",
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    sourceInfo: SourceInfo = getSourceInfo(),
    function: ContextLambda
) = RootContext(Context(name, null, sourceInfo, isolation), order, ignored, function)

expect fun getSourceInfo(): SourceInfo

data class RootContext(
    val context: Context,
    override val order: Int = 0,
    val ignored: Ignored?,
    val function: ContextLambda
) : LoadResult, failgood.internal.Path {
    val sourceInfo: SourceInfo
        get() = context.sourceInfo!! // in the root context we are sure that we always have a sourceInfo
    override val path: List<String>
        get() = listOf(context.name)
}

/* something that contains tests */
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
