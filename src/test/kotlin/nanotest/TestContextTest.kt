package nanotest

import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

object TestContextTest {
    private val automaticallyNamedContext = Context {}

    val context = Context {
        test("root context can get name from enclosing object") {
            expectThat(automaticallyNamedContext.name).isEqualTo(TestContextTest::class.simpleName)
        }
        test("can close resources") {
            val events = mutableListOf<String>()
            var closeCalled = false
            val closable = AutoCloseable { closeCalled = true }


            var resource: AutoCloseable? = null
            Suite {
                resource = autoClose(closable) {
                    it.close()
                    events.add("close callback")
                }
                test("a test") {
                    events.add("test")
                }
            }.run()
            expectThat(events).containsExactly("test", "close callback")
            expectThat(resource).isEqualTo(closable)
            expectThat(closeCalled).isTrue()
        }
    }
}
