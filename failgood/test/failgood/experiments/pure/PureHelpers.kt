package failgood.experiments.pure

fun <ContextGiven> root(
    name: String,
    given: suspend GivenDSL<Unit>.() -> ContextGiven,
    vararg children: Node<ContextGiven>
) = Context(name, given, children.asList())

fun <ParentGiven, ContextGiven> context(
    name: String,
    given: suspend GivenDSL<ParentGiven>.() -> ContextGiven,
    vararg children: Node<ContextGiven>
) = Context(name, given, children.asList())

fun <ContextGiven> root(
    name: String,
    given: suspend GivenDSL<Unit>.() -> ContextGiven,
    children: List<Node<ContextGiven>>
) = Context(name, given, children)

fun <ParentGiven, ContextGiven> context(
    name: String,
    given: suspend GivenDSL<ParentGiven>.() -> ContextGiven,
    children: List<Node<ContextGiven>>
) = Context(name, given, children)

fun <GivenType> test(name: String, function: suspend GivenType.() -> Unit) = Test(name, function)

sealed interface Node<out GivenType>

data class Test<GivenType>(val name: String, val function: suspend GivenType.() -> Unit) :
    Node<GivenType>

data class Context<ParentGiven, ContextGiven>(
    val name: String,
    val given: suspend GivenDSL<ParentGiven>.() -> ContextGiven,
    val children: List<Node<ContextGiven>>
) : Node<ParentGiven>

interface GivenDSL<GivenType> {
    val given: GivenType
}
