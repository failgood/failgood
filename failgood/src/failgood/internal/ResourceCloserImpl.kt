package failgood.internal

import failgood.SuspendAutoCloseable
import failgood.TestDependency
import failgood.TestResult
import failgood.dsl.ContextOnlyResourceDSL
import failgood.dsl.TestDSL
import failgood.jvm.JVMTestDependency
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentLinkedQueue

internal class ResourceCloserImpl(private val scope: CoroutineScope) :
    ResourcesCloser, ContextOnlyResourceDSL {
    private val logger = KotlinLogging.logger {}
    override fun <T> autoClose(wrapped: T, closeFunction: suspend (T) -> Unit): T {
        addCloseable(Closer(wrapped, closeFunction))
        return wrapped
    }

    override fun afterEach(function: suspend TestDSL.(TestResult) -> Unit) {
        addAfterEach(function)
    }

    override fun addAfterEach(function: suspend TestDSL.(TestResult) -> Unit) {
        afterEachCallbacks.add(function)
    }

    override suspend fun <T> dependency(
        creator: suspend () -> T,
        closer: suspend (T) -> Unit
    ): TestDependency<T> {
        val result = scope.async(Dispatchers.IO) { kotlin.runCatching { creator() } }
        addCloseable(Closer(result) { closer(result.await().getOrThrow()) })
        return JVMTestDependency(result)
    }

    override fun <T : AutoCloseable> autoClose(autoCloseable: T): T =
        autoClose(autoCloseable) { it.close() }

    override fun <T : SuspendAutoCloseable> autoClose(autoCloseable: T): T =
        autoCloseable.also { addCloseable(it) }

    override fun addCloseable(autoCloseable: SuspendAutoCloseable) {
        closeables.add(autoCloseable)
    }

    override suspend fun closeAutoCloseables() {
        logger.debug { "calling ${closeables.size} auto closeables" }
        closeables.reversed().forEach { it.close() }
    }

    override suspend fun callAfterEach(testDSL: TestDSL, testResult: TestResult) {
        var error: Throwable? = null
        logger.debug { "calling ${afterEachCallbacks.size} after each blocks" }
        afterEachCallbacks.forEach {
            try {
                it.invoke(testDSL, testResult)
            } catch (e: Throwable) {
                if (error == null) error = e
            }
        }
        error?.let { throw it }
    }

    private val closeables = ConcurrentLinkedQueue<SuspendAutoCloseable>()
    private val afterEachCallbacks = ConcurrentLinkedQueue<suspend TestDSL.(TestResult) -> Unit>()

    private class Closer<T>(private val closeable: T, val closer: suspend (T) -> Unit) :
        SuspendAutoCloseable {
        override suspend fun close() {
            closer(closeable)
        }
    }
}
