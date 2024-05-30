package failgood.internal

import failgood.internal.sysinfo.cpus
import failgood.internal.util.getenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val envParallelism = getenv("FAILGOOD_PARALLELISM")?.toInt()

class SuiteExecutionContext(parallelismOverride: Int? = null) : AutoCloseable {
    // constructor parameter overrides system env variable overrides number of cpus autodetect
    val parallelism = parallelismOverride ?: envParallelism ?: cpus()
    private val threadPool: ExecutorService =
        if (parallelism > 1) Executors.newWorkStealingPool(parallelism)
        else Executors.newSingleThreadExecutor()

    val coroutineDispatcher: ExecutorCoroutineDispatcher = threadPool.asCoroutineDispatcher()
    val scope = CoroutineScope(coroutineDispatcher)

    override fun close() {
        scope.cancel()
        coroutineDispatcher.close()
        threadPool.awaitTermination(10, TimeUnit.SECONDS)
        threadPool.shutdown()
    }
}
