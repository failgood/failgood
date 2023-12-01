@file:Suppress("unused", "UNUSED_ANONYMOUS_PARAMETER", "UNUSED_PARAMETER")

package failgood.experiments.pure

/** possible syntax for a suite without side effects */
class PureTest {
    open class Given(val mongoDB: MongoDB)

    private val withListOfNodes =
        context(
            "root",
            given = { Given(MongoDB()) },
            listOf(
                test("a test") { mongoDB.flush() },
                context("a subcontext", {}, listOf(test("another test") {})),
                context(
                    "dynamic tests",
                    { UpperCaser() },
                    listOf(Pair("chris", "CHRIS"), Pair("freddy", "FREDDY")).map {
                        (name, uppercaseName) ->
                        test("uppercases $name to $uppercaseName") {
                            assert(this.toUpperCase(name) == uppercaseName)
                        }
                    }
                )
            )
        )
    private val withVarargNodes =
        context(
            "root",
            given = { Given(MongoDB()) },
            test("a test") { mongoDB.flush() },
            context("a subcontext", {}, test("another test") {}),
            context(
                "dynamic tests",
                { UpperCaser() },
                listOf(Pair("chris", "CHRIS"), Pair("freddy", "FREDDY")).map { (name, uppercaseName)
                    ->
                    test("uppercases $name to $uppercaseName") {
                        assert(this.toUpperCase(name) == uppercaseName)
                    }
                }
            )
        )

    class UpperCaser {
        @Suppress("SameReturnValue") fun toUpperCase(name: String): String = "not yet"
    }

    class MongoDB {
        fun flush() {
            TODO("Not yet implemented")
        }
    }
}
