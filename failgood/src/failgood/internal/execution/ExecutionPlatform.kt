package failgood.internal.execution

import failgood.TestDescription
import failgood.internal.FailedTestCollectionExecution
import failgood.internal.ResourceCloserImpl
import failgood.internal.ResourcesCloser
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.MDC

internal actual fun executionContext(): CoroutineContext = MDCContext()

internal actual fun withCurrentTestContext(testDescription: TestDescription, block: () -> Unit) {
    val closable =
        if (MDC.get("test") == null) {
            MDC.putCloseable("test", testDescription.niceString())
        } else {
            null
        }
    try {
        block()
    } finally {
        closable?.close()
    }
}

internal actual fun createResourcesCloser(scope: CoroutineScope): ResourcesCloser =
    ResourceCloserImpl(scope)

internal actual fun platformNanoTime(): Long = System.nanoTime()

internal actual fun preloadFailedTestCollectionExecution() {
    Class.forName(FailedTestCollectionExecution::class.qualifiedName)
}
