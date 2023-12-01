package failgood.experiments.pure

fun <GivenType> context(
    name: String,
    given: suspend () -> GivenType,
    children: List<Node<GivenType>>
) = Context(name, given, children)

fun <GivenType> test(name: String, function: suspend GivenType.() -> Unit) = Test(name, function)

sealed interface Node<out GivenType>

data class Test<GivenType>(val name: String, val function: suspend GivenType.() -> Unit) :
    Node<GivenType>

data class Context<GivenType>(
    val name: String,
    val given: suspend () -> GivenType,
    val children: List<Node<GivenType>>
) : Node<Nothing>

fun <GivenType> context(
    name: String,
    given: suspend () -> GivenType,
    vararg children: Node<GivenType>
) = Context(name, given, children.asList())
