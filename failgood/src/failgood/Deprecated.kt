package failgood

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
