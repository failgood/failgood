package failgood.assert

fun <T> Iterable<T>.containsExactlyInAnyOrder(elements: Collection<T>) =
    this.toSet() == elements.toSet()

fun <T> Iterable<T>.containsExactlyInAnyOrder(vararg elements: T) =
    this.containsExactlyInAnyOrder(elements.asList())

fun <T> List<T>.endsWith(elements: List<T>) = this.takeLast(elements.size) == elements
fun <T> List<T>.endsWith(vararg elements: T) = this.endsWith(elements.asList())
