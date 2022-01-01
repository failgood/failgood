package failgood

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import strikt.api.expectThat
import strikt.assertions.containsExactly

@Test
class CoroutinesTest {
    val context = describe("a coroutine scope inside failgood tests") {
        it("works as expected") {
            val events = mutableListOf<String>()
            runBlocking(Dispatchers.Default) {
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
