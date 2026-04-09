package failgood.internal.execution

import failgood.TestDescription
import failgood.internal.ResourcesCloser
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope

internal expect fun executionContext(): CoroutineContext

internal expect fun withCurrentTestContext(testDescription: TestDescription, block: () -> Unit)

internal expect fun createResourcesCloser(scope: CoroutineScope): ResourcesCloser

internal expect fun platformNanoTime(): Long

internal expect fun preloadFailedTestCollectionExecution()
