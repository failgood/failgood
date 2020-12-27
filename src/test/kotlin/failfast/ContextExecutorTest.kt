package failfast

import failfast.internal.ContextExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.runBlocking
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

object ContextExecutorTest {
    val context = describe(ContextExecutor::class) {
        test("returns the number of tests") {
            val ctx = RootContext("root context") {
                test("test 1") {
                }
                test("test 2") {
                }
                context("context 1") {
                    context("context 2") {
                        test("test 3") {
                        }
                    }
                }
                context("context 4") {
                    test("test 4") {
                    }
                }

            }
            val testResultChannel = Channel<TestResult>(UNLIMITED)
            @Suppress("BlockingMethodInNonBlockingContext")
            runBlocking(Dispatchers.Unconfined) {
                expectThat(ContextExecutor(ctx, testResultChannel, this).execute()).isEqualTo(4)
            }

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

