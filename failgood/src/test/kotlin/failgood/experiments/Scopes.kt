package failgood.experiments

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val start = System.currentTimeMillis()
// playground to check that coroutines work as expected
fun println(s: String) {
    kotlin.io.println("" + (System.currentTimeMillis() - start) + " " + s)
}
suspend fun main() {
    runBlocking(Dispatchers.Default) {
        val outerScope = this
        coroutineScope {
            outerScope.launch {
                delay(1000)
                println("end of async")
            }
            println("end of scope")
        }
        println("finished scope")
    }
    println("finished runblocking")
}
