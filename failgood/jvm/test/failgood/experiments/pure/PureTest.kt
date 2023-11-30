@file:Suppress("unused", "UNUSED_ANONYMOUS_PARAMETER", "UNUSED_PARAMETER")

package failgood.experiments.pure

/** possible syntax for a suite without side effects */
class PureTest {
    private val withListOfNodes =
        context(
            "root",
            given = { MongoDB() },
            listOf(
                test("a test") { mongoDb: MongoDB -> },
                context("a subcontext", {}, listOf(test("another test") {})),
                context(
                    "dynamic tests",
                    { UpperCaser() },
                    listOf(Pair("chris", "CHRIS"), Pair("freddy", "FREDDY")).map {
                        (name, uppercaseName) ->
                        test("uppercases $name to $uppercaseName") { uppercaser ->
                            assert(uppercaser.toUpperCase(name) == uppercaseName)
                        }
                    }
                )
            )
        )
    private val withVarargNodes =
        context(
            "root",
            given = { MongoDB() },
            test("a test") { mongoDb: MongoDB -> },
            context("a subcontext", {}, test("another test") {}),
            context(
                "dynamic tests",
                { UpperCaser() },
                *(listOf(Pair("chris", "CHRIS"), Pair("freddy", "FREDDY"))
                    .map { (name, uppercaseName) ->
                        test("uppercases $name to $uppercaseName") { uppercaser: UpperCaser ->
                            assert(uppercaser.toUpperCase(name) == uppercaseName)
                        }
                    }
                    .toTypedArray())
            )
        )

    class UpperCaser {
        @Suppress("SameReturnValue") fun toUpperCase(name: String): String = "not yet"
    }

    class MongoDB

    private fun <GivenType> context(
        name: String,
        given: suspend () -> GivenType,
        children: List<Node<GivenType>>
    ) = Context(name, given, children)

    private fun <GivenType> test(name: String, function: suspend (GivenType) -> Unit) =
        Test(name, function)

    sealed interface Node<out GivenType>

    private data class Test<GivenType>(
        val name: String,
        val function: suspend (GivenType) -> Unit
    ) : Node<GivenType>

    private data class Context<GivenType>(
        val name: String,
        val given: suspend () -> GivenType,
        val children: List<Node<GivenType>>
    ) : Node<Nothing>

    private fun <GivenType> context(
        name: String,
        given: suspend () -> GivenType,
        vararg children: Node<GivenType>
    ) = Context(name, given, children.asList())
}
