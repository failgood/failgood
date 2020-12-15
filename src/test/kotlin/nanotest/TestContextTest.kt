package nanotest

import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

fun main() {
    Suite(listOf(TestContextTest.context)).run().check()
}
object TestContextTest {
    val context = Context {
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
        xtest("every test gets a fresh context") {
            val events = mutableListOf<String>()
            TestContext("the context") {
                events.add("root context")
                test("test 1") {
                    events.add("test 1")
                }
                test("test 2") {
                    events.add("test 2")
                }
            }.execute()
            expectThat(events).containsExactly("root context", "test 1", "root context", "test 2")
        }
    }
}
