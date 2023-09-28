package failgood.internal

import failgood.TestDSL
import failgood.TestDependency
import failgood.TestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentLinkedQueue

internal class ResourceCloserImpl(private val scope: CoroutineScope) : ResourcesCloser {
    override fun <T> autoClose(wrapped: T, closeFunction: suspend (T) -> Unit): T {
        addClosable(SuspendAutoCloseable(wrapped, closeFunction))
        return wrapped
    }

    override fun afterEach(function: suspend TestDSL.(TestResult) -> Unit) {
        addAfterEach(function)
    }

    override fun addAfterEach(function: suspend TestDSL.(TestResult) -> Unit) {
        afterEachCallbacks.add(function)
    }

    override suspend fun <T> dependency(creator: suspend () -> T, closer: suspend (T) -> Unit): TestDependency<T> {
        val result = scope.async(Dispatchers.IO) { kotlin.runCatching { creator() } }
        addClosable(SuspendAutoCloseable(result) { closer(result.await().getOrThrow()) })
        return TestDependency(result)
    }

    override fun <T : AutoCloseable> autoClose(wrapped: T): T = autoClose(wrapped) { it.close() }

    override fun <T> addClosable(autoCloseable: SuspendAutoCloseable<T>) {
        closeables.add(autoCloseable)
    }

    override suspend fun closeAutoCloseables() {
        closeables.reversed().forEach { it.close() }
    }
    override suspend fun callAfterEach(testDSL: TestDSL, testResult: TestResult) {
        var error: Throwable? = null
        afterEachCallbacks.forEach {
            try {
                it.invoke(testDSL, testResult)
            } catch (e: Throwable) {
                if (error == null)
                    error = e
            }
        }
        error?.let { throw it }
    }

    private val closeables = ConcurrentLinkedQueue<SuspendAutoCloseable<*>>()
    private val afterEachCallbacks = ConcurrentLinkedQueue<suspend TestDSL.(TestResult) -> Unit>()
}