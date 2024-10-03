package failgood

import failgood.dsl.ContextFunction
import failgood.dsl.ContextFunctionWithGiven
import failgood.internal.ContextPath
import kotlin.reflect.KClass

data class TestDescription(val context: Context, val testName: String, val sourceInfo: SourceInfo) {
    internal constructor(
        testPath: ContextPath,
        sourceInfo: SourceInfo
    ) : this(testPath.container, testPath.name, sourceInfo)

    fun niceString(): String {
        return "${context.stringPath()} > $testName"
    }
}

// https://youtrack.jetbrains.com/issue/KTIJ-27236/False-positive-Java-inspection-Method-always-returns-the-same-value-with-on-Kotlin-code
@Suppress("SameReturnValue")
internal sealed interface LoadResult {
    val order: Int
}

internal data class CouldNotLoadTestCollection(val reason: Throwable, val kClass: KClass<out Any>) :
    LoadResult {
    override val order: Int
        get() = 0
}

fun TestCollection(
    name: String = "root",
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    sourceInfo: SourceInfo = callerSourceInfo(),
    addClassName: Boolean = false,
    function: ContextFunction
): TestCollection<Unit> =
    TestCollection(name, ignored, order, isolation, sourceInfo, addClassName, {}, function)

fun <RootGiven> TestCollection(
    name: String = "root",
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    sourceInfo: SourceInfo = callerSourceInfo(),
    addClassName: Boolean = false,
    given: (suspend () -> RootGiven),
    function: ContextFunctionWithGiven<RootGiven>
): TestCollection<RootGiven> =
    TestCollection(
        Context(name, null, sourceInfo, isolation),
        order,
        ignored,
        addClassName,
        given,
        function = function)

data class TestCollection<RootGiven>(
    val rootContext: Context,
    override val order: Int = 0,
    val ignored: Ignored?,
    val addClassName: Boolean = false,
    val given: (suspend () -> RootGiven) = {
        @Suppress("UNCHECKED_CAST")
        Unit as RootGiven
    },
    val function: ContextFunctionWithGiven<RootGiven>
) : LoadResult, failgood.internal.Path {
    val sourceInfo: SourceInfo
        get() =
            rootContext
                .sourceInfo!! // in the root context we are sure that we always have a sourceInfo

    override val path: List<String>
        get() = listOf(rootContext.name)
}

/** something that contains tests */
data class Context(
    val name: String,
    val parent: Context? = null,
    val sourceInfo: SourceInfo? = null,
    val isolation: Boolean = true,
    val displayName: String = name
) {
    init {
        if (name.isBlank()) throw IllegalArgumentException("name must not be blank")
    }

    companion object {
        fun fromPath(path: List<String>): Context {
            return Context(path.last(), if (path.size == 1) null else fromPath(path.dropLast(1)))
        }
    }

    val parents: List<Context> = parent?.parents?.plus(parent) ?: listOf()

    /** this is used for example for filtering */
    val path: List<String> = parent?.path?.plus(name) ?: listOf(name)

    /** path for displaying to the user */
    private val displayPath: List<String> = parent?.path?.plus(displayName) ?: listOf(displayName)

    fun stringPath(): String = displayPath.joinToString(" > ")
}
