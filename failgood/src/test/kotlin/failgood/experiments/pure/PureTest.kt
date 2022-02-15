@file:Suppress("unused")

package failgood.experiments.pure

/**
 * a way to create a suite without side effects
 */
class PureTest {
    private val suite = context(
        "root",
        listOf(
            test("a test") {},
            context(
                "a subcontext",
                listOf(
                    test("another test") {},
                )
            )
        )
    )
    private fun context(name: String, listOf: List<Node>) = Context(name, listOf)

    private fun test(name: String, function: suspend () -> Unit) = Test(name, function)

    sealed interface Node
    private data class Test(val name: String, val function: suspend () -> Unit) : Node
    private data class Context(val name: String, val children: List<Node>) : Node
}
