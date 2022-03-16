package failgood.internal

import failgood.cpus
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
private val envParallelism: Int? = System.getenv("FAILGOOD_PARALLELISM")?.toInt()

class SuiteExecutionContext(parallelismOverride: Int? = null) : AutoCloseable {
    // constructor parameter overrides system env variable overrides number of cpus autodetect
    private val parallelism = parallelismOverride ?: envParallelism ?: cpus().coerceAtLeast(8)
    private val threadPool: ExecutorService = if (parallelism > 1)
        Executors.newWorkStealingPool(parallelism)
    else
        Executors.newSingleThreadExecutor()

    val coroutineDispatcher = threadPool.asCoroutineDispatcher()
    override fun close() {
        coroutineDispatcher.close()
        threadPool.awaitTermination(10, TimeUnit.SECONDS)
        threadPool.shutdown()
    }
}
