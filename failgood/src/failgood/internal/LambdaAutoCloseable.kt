package failgood.internal

import failgood.SuspendAutoCloseable

internal class LambdaAutoCloseable<T>(private val wrapped: T, val closer: suspend (T) -> Unit) :
    SuspendAutoCloseable {
    override suspend fun close() {
        closer(wrapped)
    }
}
