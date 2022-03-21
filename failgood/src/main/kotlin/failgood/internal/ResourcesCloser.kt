package failgood.internal

import failgood.ResourcesDSL
import failgood.TestDependency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentLinkedQueue

internal class ResourcesCloser(private val scope: CoroutineScope) : ResourcesDSL {
    override fun <T> autoClose(wrapped: T, closeFunction: suspend (T) -> Unit): T {
        addClosable(SuspendAutoCloseable(wrapped, closeFunction))
        return wrapped
    }

    override fun afterEach(function: suspend () -> Unit) {
        addAfterEach(function)
    }

    private fun addAfterEach(function: suspend () -> Unit) {
        afterEachCallbacks.add(function)
    }

    override suspend fun <T> dependency(creator: suspend () -> T, closer: suspend (T) -> Unit): TestDependency<T> {
        val result = scope.async(Dispatchers.IO) { creator() }
        addClosable(SuspendAutoCloseable(result) { closer(result.await()) })
        return TestDependency(result)
    }

    override fun <T : AutoCloseable> autoClose(wrapped: T): T = autoClose(wrapped) { it.close() }

    private fun <T> addClosable(autoCloseable: SuspendAutoCloseable<T>) {
        closeables.add(autoCloseable)
    }

    suspend fun close() {
        closeables.reversed().forEach { it.close() }
        afterEachCallbacks.reversed().forEach {
            it.invoke()
        }
    }

    private val closeables = ConcurrentLinkedQueue<SuspendAutoCloseable<*>>()
    private val afterEachCallbacks = ConcurrentLinkedQueue<suspend () -> Unit>()
}
