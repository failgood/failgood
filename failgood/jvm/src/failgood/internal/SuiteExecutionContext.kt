package failgood.internal

import failgood.cpus
import failgood.util.getenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
private val envParallelism = getenv("FAILGOOD_PARALLELISM")?.toInt()

class SuiteExecutionContext(parallelismOverride: Int? = null) : AutoCloseable {
    // constructor parameter overrides system env variable overrides number of cpus autodetect
    private val parallelism = parallelismOverride ?: envParallelism ?: cpus()
    private val threadPool: ExecutorService = if (parallelism > 1)
        Executors.newWorkStealingPool(parallelism)
    else
        Executors.newSingleThreadExecutor()

    val coroutineDispatcher = threadPool.asCoroutineDispatcher()
    val scope = CoroutineScope(coroutineDispatcher)
    override fun close() {
        scope.cancel()
        coroutineDispatcher.close()
        threadPool.awaitTermination(10, TimeUnit.SECONDS)
        threadPool.shutdown()
    }
}
