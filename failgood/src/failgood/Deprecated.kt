@file:Suppress("unused")

package failgood

import failgood.dsl.ContextFunctionWithGiven

@Deprecated(
    message = "moved to failgood.dsl",
    replaceWith = ReplaceWith("ContextDSL", "failgood.dsl.ContextDSL")
)
typealias ContextDSL<T> = failgood.dsl.ContextDSL<T>

@Deprecated(
    message = "moved to failgood.dsl",
    replaceWith = ReplaceWith("ResourcesDSL", "failgood.dsl.ResourcesDSL")
)
typealias ResourcesDSL = failgood.dsl.ResourcesDSL

@Deprecated(
    message = "renamed",
    replaceWith = ReplaceWith("TestCollection<Unit>")
)
typealias RootContext = TestCollection<Unit>
@Deprecated(
    message = "renamed",
    replaceWith = ReplaceWith("TestCollection<Given>")
)
typealias RootContextWithGiven<Given> = TestCollection<Given>

@Deprecated(
    message = "renamed",
    replaceWith = ReplaceWith("TestCollection(name, ignored, order, isolation, sourceInfo, addClassName, given, function)")
)
fun <RootGiven> RootContextWithGiven(
    name: String = "root",
    ignored: Ignored? = null,
    order: Int = 0,
    isolation: Boolean = true,
    sourceInfo: SourceInfo = callerSourceInfo(),
    addClassName: Boolean = false,
    given: (suspend () -> RootGiven),
    function: ContextFunctionWithGiven<RootGiven>
): TestCollection<RootGiven> =
    TestCollection(name, ignored, order, isolation, sourceInfo, addClassName, given, function)

