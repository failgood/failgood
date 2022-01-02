package failgood.experiments

import failgood.ContextLambda
import failgood.RootContext
import failgood.Test

/*
this is not yet implemented, but could be cool
 */
class TestContextOnTopLevelJunitTest {
    @Test
    fun `top level contexts can be declared as function`() =
        describe {
            it("makes it possible to have multiple top level contexts in a class and run them separately from idea") {}
        }
}

fun describe(
    disabled: Boolean = false,
    order: Int = 0,
    isolation: Boolean = true,
    function: ContextLambda
):
    RootContext = RootContext("todo", disabled, order, isolation, function = function)
