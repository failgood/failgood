package failgood.assert

fun <T : Comparable<T>> Iterable<T>.containsExactlyInAnyOrder(elements: Collection<T>) =
    this.sorted() == elements.sorted()
