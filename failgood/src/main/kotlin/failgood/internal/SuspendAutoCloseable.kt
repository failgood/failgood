package failgood.internal

class SuspendAutoCloseable<T>(private val wrapped: T, val closer: suspend (T) -> Unit) {
    suspend fun close() {
        closer(wrapped)
    }
}
