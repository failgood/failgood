package failfast

import failfast.internal.ContextExecutor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

object ContextExecutorTest {
    val context = describe(ContextExecutor::class) {
        test("returns contexts and number of tests") {
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

            coroutineScope {
                val testResultChannel = Channel<TestResult>(UNLIMITED)
                val rootContext = Context("root context", null)
                val context1 = Context("context 1", rootContext)
                val context2 = Context("context 2", context1)
                val context4 = Context("context 4", rootContext)
                val contextInfo = ContextExecutor(ctx, testResultChannel, this).execute()
                expectThat(contextInfo.tests).isEqualTo(4)
                expectThat(contextInfo.contexts).containsExactlyInAnyOrder(rootContext, context1, context2, context4)

                // we expect 4 times success
                expectThat(testResultChannel.receive()).isA<Success>()
                expectThat(testResultChannel.receive()).isA<Success>()
                expectThat(testResultChannel.receive()).isA<Success>()
                expectThat(testResultChannel.receive()).isA<Success>()
            }



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

