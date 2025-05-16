package failgood.assert

fun <T> Iterable<T>.containsExactlyInAnyOrder(elements: Collection<T>): Boolean =
    this.toSet() == elements.toSet()

fun <T> Iterable<T>.containsExactlyInAnyOrder(vararg elements: T): Boolean =
    this.containsExactlyInAnyOrder(elements.asList())

fun <T> List<T>.endsWith(elements: List<T>): Boolean = this.takeLast(elements.size) == elements

fun <T> List<T>.endsWith(vararg elements: T): Boolean = this.endsWith(elements.asList())
