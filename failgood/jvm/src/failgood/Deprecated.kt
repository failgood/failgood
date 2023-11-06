package failgood

@Deprecated(
    message = "moved to failgood.dsl",
    replaceWith = ReplaceWith("ContextDSL", "failgood.dsl.ContextDSL")
)
typealias ContextDSL<ContextDependency, Given> = failgood.dsl.ContextDSL<ContextDependency, Given>

@Deprecated(
    message = "moved to failgood.dsl",
    replaceWith = ReplaceWith("ResourcesDSL", "failgood.dsl.ResourcesDSL")
)
typealias ResourcesDSL = failgood.dsl.ResourcesDSL
