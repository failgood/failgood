package failgood

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import strikt.api.expectThat
import strikt.assertions.containsExactly
import java.util.concurrent.Executors

@Test
class CoroutinesTest {
    val tests =
        testCollection("a coroutine scope inside failgood tests") {
            it("works as expected") {
                val events = mutableListOf<String>()
                val newWorkStealingPool =
                    autoClose(Executors.newWorkStealingPool(2)) { it.shutdown() }
                val dispatcher = autoClose(newWorkStealingPool.asCoroutineDispatcher())
                runBlocking(dispatcher) {
                    val outerScope = this
                    coroutineScope {
                        outerScope.launch {
                            delay(100)
                            events.add("after-delay")
                        }
                        events.add("after-async")
                    }
                    events.add("after-scope")
                }
                expectThat(events).containsExactly("after-async", "after-scope", "after-delay")
            }
        }
}
