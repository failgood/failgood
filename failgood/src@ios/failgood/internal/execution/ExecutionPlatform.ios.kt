package failgood.internal.execution

import failgood.SuspendAutoCloseable
import failgood.TestDependency
import failgood.TestDescription
import failgood.TestResult
import failgood.dsl.ContextOnlyResourceDSL
import failgood.dsl.TestDSL
import failgood.internal.ResourcesCloser
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSLock
import platform.Foundation.NSProcessInfo

internal actual fun executionContext(): CoroutineContext = EmptyCoroutineContext

internal actual fun withCurrentTestContext(testDescription: TestDescription, block: () -> Unit) {
    block()
}

internal actual fun createResourcesCloser(scope: CoroutineScope): ResourcesCloser =
    IosResourceCloser(scope)

internal actual fun platformNanoTime(): Long =
    (NSProcessInfo.processInfo.systemUptime * 1_000_000_000.0).toLong()

internal actual fun preloadFailedTestCollectionExecution() {}

private class IosResourceCloser(private val scope: CoroutineScope) :
    ResourcesCloser, ContextOnlyResourceDSL {
    private val lock = NSLock()
    private val closeables = mutableListOf<SuspendAutoCloseable>()
    private val afterEachCallbacks = mutableListOf<suspend TestDSL.(TestResult) -> Unit>()

    override fun <T> autoClose(wrapped: T, closeFunction: suspend (T) -> Unit): T {
        addCloseable(Closer(wrapped, closeFunction))
        return wrapped
    }

    override fun afterEach(function: suspend TestDSL.(TestResult) -> Unit) {
        addAfterEach(function)
    }

    override fun addAfterEach(function: suspend TestDSL.(TestResult) -> Unit) {
        withLock { afterEachCallbacks += function }
    }

    override suspend fun <T> dependency(
        creator: suspend () -> T,
        closer: suspend (T) -> Unit,
    ): TestDependency<T> {
        val result = scope.async(Dispatchers.Default) { runCatching { creator() } }
        addCloseable(Closer(result) { closer(result.await().getOrThrow()) })
        return DeferredTestDependency(result)
    }

    override fun <T : AutoCloseable> autoClose(autoCloseable: T): T =
        autoClose(autoCloseable) { it.close() }

    override fun <T : SuspendAutoCloseable> autoClose(autoCloseable: T): T =
        autoCloseable.also { addCloseable(it) }

    override fun addCloseable(autoCloseable: SuspendAutoCloseable) {
        withLock { closeables += autoCloseable }
    }

    override suspend fun closeAutoCloseables() {
        withLock { closeables.toList() }.asReversed().forEach { it.close() }
    }

    override suspend fun callAfterEach(testDSL: TestDSL, testResult: TestResult) {
        var error: Throwable? = null
        withLock { afterEachCallbacks.toList() }
            .forEach {
                try {
                    it.invoke(testDSL, testResult)
                } catch (e: Throwable) {
                    if (error == null) error = e
                }
            }
        error?.let { throw it }
    }

    private class Closer<T>(private val closeable: T, private val closer: suspend (T) -> Unit) :
        SuspendAutoCloseable {
        override suspend fun close() {
            closer(closeable)
        }
    }

    private fun <T> withLock(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}

private class DeferredTestDependency<T>(private val dependency: Deferred<Result<T>>) :
    TestDependency<T> {
    override fun getValue(owner: Any?, property: kotlin.reflect.KProperty<*>): T {
        return runBlocking { dependency.await().getOrThrow() }
    }
}
