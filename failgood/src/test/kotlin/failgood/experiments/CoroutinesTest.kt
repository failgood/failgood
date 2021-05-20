package failgood.experiments

import failgood.uptime
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

fun main() {
    asyncTest()
//    flowTest()
}

private fun asyncTest() {
    val threadPool = Executors.newWorkStealingPool(1000)
    threadPool.asCoroutineDispatcher()
        .use { dispatcher ->
            runBlocking(dispatcher) {
                (0 until 1000)
                    .map {
                        async {
                            @Suppress("BlockingMethodInNonBlockingContext")
                            Thread.sleep(1000)
                        }
                    }
                    .awaitAll()
                println(uptime() + " " + threadPool)
            }
        }
}

// no idea how to do multithreading with flows
private fun flowTest() {
    val threadPool = Executors.newWorkStealingPool(1000)
    threadPool.asCoroutineDispatcher()
        .use { dispatcher ->
            runBlocking(dispatcher) {
                (0 until 1000).asFlow().flowOn(dispatcher).map {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    Thread.sleep(1000)
                    it
                }.toCollection(mutableListOf())
            }
            println(uptime() + " " + threadPool)
        }
}

