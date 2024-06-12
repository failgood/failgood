package failgood.experiments

import failgood.Ignored
import failgood.Test
import failgood.testsAbout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

val threadLocal = ThreadLocal<String?>() // declare thread-local variable

/*
  Just playing around with coroutine threadlocals
 */
@Test
object CoroutineThreadLocalExperiment {
    val tests = testsAbout(
        "Thread Locals with coroutines",
        ignored = Ignored.Because("this is just an experiment that does not work")
    ) {
        it("work in nested contexts") {
            threadLocal.set("test-scope")
            coroutineScope {
                withContext(Dispatchers.Default + threadLocal.asContextElement()) {
                    assert(myFunction() == "test-scope")
                }
            }
        }
    }

    // this method simulates non coroutine code that starts a thread and then tries to get the ThreadLocal
    // for example like a handler in a web server. It does not work and I guess there is no way
    // to make it work.
    private fun myFunction(): String? {
        val f = CompletableFuture<String>()
        thread {
            runBlocking(Dispatchers.Default + threadLocal.asContextElement()) {
                f.complete(threadLocal.get())
            }
        }
        return f.get()
    }
}
