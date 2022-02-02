package failgood.internal

import failgood.ResourcesDSL
import failgood.TestDependency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentLinkedQueue

internal class ResourcesCloser(private val scope: CoroutineScope) : ResourcesDSL {
    override fun <T> autoClose(wrapped: T, closeFunction: suspend (T) -> Unit): T {
        add(SuspendAutoCloseable(wrapped, closeFunction))
        return wrapped
    }

    override suspend fun <T> dependency(creator: suspend () -> T, closer: suspend (T) -> Unit): TestDependency<T> {
        val result = scope.async(Dispatchers.IO) { creator() }
        add(SuspendAutoCloseable(result) { closer(result.await()) })
        return TestDependency(result)
    }

    override fun <T : AutoCloseable> autoClose(wrapped: T): T = autoClose(wrapped) { it.close() }

    private fun <T> add(autoCloseable: SuspendAutoCloseable<T>) {
        closeables.add(autoCloseable)
    }

    suspend fun close() {
        closeables.reversed().forEach { it.close() }
    }

    private val closeables = ConcurrentLinkedQueue<SuspendAutoCloseable<*>>()
}
