@file:Suppress("unused", "UNUSED_ANONYMOUS_PARAMETER")

package failgood.experiments.pure

/**
 * possible syntax for a suite without side effects
 */
class PureTest {
    private val suite = context(
        "root", { MongoDB() },
        listOf(
            test("a test") { mongoDb: MongoDB -> },
            context(
                "a subcontext", {},
                listOf(
                    test("another test") {},
                )
            )
        )
    )

    class MongoDB

    private fun <Fixture> context(name: String, fixture: suspend () -> Fixture, listOf: List<Node<Fixture>>) =
        Context(name, fixture, listOf)

    private fun <Fixture> test(name: String, function: suspend (Fixture) -> Unit) = Test(name, function)

    sealed interface Node<out Fixture>
    private data class Test<Fixture>(val name: String, val function: suspend (Fixture) -> Unit) : Node<Fixture>
    private data class Context<Fixture>(
        val name: String,
        val fixture: suspend () -> Fixture,
        val children: List<Node<Fixture>>
    ) : Node<Nothing>
}
