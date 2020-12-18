package nanotest

import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

object ContextExecutorTest {
    val context = context {
        test("runs the optimal path to each test") {
            val events = mutableListOf<String>()
            val ctx = Context("root context") {
                events.add("root context")
                test("test 1") {
                    events.add("test 1")
                }
                test("test 2") {
                    events.add("test 2")
                }
                context("context 1") {
                    events.add("context 1")

                    context("context 2") {
                        events.add("context 2")
                        test("test 3") {
                            events.add("test 3")
                        }
                    }
                }
                context("context 4") {
                    events.add("context 4")
                    test("test 4") {
                        events.add("test 4")
                    }
                }

            }
            val result: List<TestResult> = ContextExecutor(ctx).execute()
            expectThat(events).containsExactly(
                "root context", "test 1",
                "root context", "test 2",
                "root context", "context 1", "context 2", "test 3",
                "root context", "context 4", "test 4"
            )
            expectThat(result).all { isA<Success>() }

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

