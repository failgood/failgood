import failgood.describe
import failgood.dsl.ContextLambda

fun xdescribe(name: String, function: ContextLambda) = describe(name, function = function)
