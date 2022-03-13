package failgood.internal

import failgood.cpus
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SuiteExecutionContext(parallelism: Int = cpus()) : AutoCloseable {
    val threadPool: ExecutorService = if (parallelism > 1)
        Executors.newWorkStealingPool(parallelism)
    else
        Executors.newSingleThreadExecutor()

    override fun close() {
        threadPool.awaitTermination(0, TimeUnit.SECONDS)
        threadPool.shutdown()
    }
}
