package failfast

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

object ContextExecutorTest {
    val context = context {
        test("returns the number of tests") {
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

            val testResultChannel = Channel<TestResult>(UNLIMITED)
            expectThat(ContextExecutor(ctx, testResultChannel).execute()).isEqualTo(4)
            expectThat(events).containsExactly(
                "root context", "test 1",
                "root context", "test 2",
                "root context", "context 1", "context 2", "test 3",
                "root context", "context 4", "test 4"
            )

            // we expect 4 times success
            expectThat(testResultChannel.receive()).isA<Success>()
            expectThat(testResultChannel.receive()).isA<Success>()
            expectThat(testResultChannel.receive()).isA<Success>()
            expectThat(testResultChannel.receive()).isA<Success>()

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

