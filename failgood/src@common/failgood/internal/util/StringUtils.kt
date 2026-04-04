package failgood.internal.util

internal fun pluralize(count: Int, item: String) = if (count == 1) "1 $item" else "$count ${item}s"
