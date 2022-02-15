@file:Suppress("unused")

package failgood.experiments.pure

/**
 * possible syntax for a suite without side effects
 */
class PureTest {
    private val suite = context(
        "root",
        listOf(
            dependency("mongodb") { MongoDB() },
            test("a test") {},
            test("a test that uses mongodb") { mongodb: MongoDB -> },
            context(
                "a subcontext",
                listOf(
                    test("another test") {},
                )
            )
        )
    )

    private fun <T> test(name: String, function: suspend (T) -> Unit) = Test1(name, function)

    private fun <T> dependency(name: String, function: () -> T) = Dependency(name, function)

    class MongoDB

    private fun context(name: String, listOf: List<Node>) = Context(name, listOf)

    private fun test(name: String, function: suspend () -> Unit) = Test(name, function)

    sealed interface Node
    private data class Test1<T>(val name: String, val function: suspend (T) -> Unit) : Node
    private data class Test(val name: String, val function: suspend () -> Unit) : Node
    private data class Context(val name: String, val children: List<Node>) : Node
    private data class Dependency<T>(val name: String, val factory: suspend () -> T) : Node
}
