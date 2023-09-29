package failgood.experiments.toplevel

import failgood.Ignored
import failgood.RootContext
import failgood.Test
import failgood.dsl.ContextLambda

/*
this already works, but it's not yet possible to run via the icon on the method. it is possible to run via the class
 */
class TestContextsAsMethodsExperiment {
    @Test
    fun `top level contexts can be declared as function`() =
        describe {
            it("makes it possible to have multiple top level contexts in a class and run them separately from idea") {}
        }
    fun describe(
        ignored: Ignored? = null,
        order: Int = 0,
        isolation: Boolean = true,
        function: ContextLambda
    ): RootContext = RootContext("todo", ignored, order, isolation, function = function)
}
