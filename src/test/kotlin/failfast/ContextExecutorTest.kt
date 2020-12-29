package failfast

import failfast.internal.ContextExecutor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

object ContextExecutorTest {
    val context = describe(ContextExecutor::class) {
        val testResultChannel = autoClose(Channel<TestResult>(UNLIMITED)) { it.close() }
        describe("with a valid root context") {
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

            it("returns number of tests") {
                coroutineScope {
                    val contextInfo = ContextExecutor(ctx, testResultChannel, this).execute()
                    expectThat(contextInfo.tests).isEqualTo(4)
                }
            }
            it("returns contexts") {
                coroutineScope {
                    val rootContext = Context("root context", null)
                    val context1 = Context("context 1", rootContext)
                    val context2 = Context("context 2", context1)
                    val context4 = Context("context 4", rootContext)
                    val contextInfo = ContextExecutor(ctx, testResultChannel, this).execute()
                    expectThat(contextInfo.contexts).containsExactlyInAnyOrder(
                        rootContext,
                        context1,
                        context2,
                        context4
                    )
                }
            }
            it("writes results to a channel") {
                coroutineScope {
                    ContextExecutor(ctx, testResultChannel, this).execute()

                    // we expect 4 times success
                    expectThat(testResultChannel.receive()).isA<Success>()
                    expectThat(testResultChannel.receive()).isA<Success>()
                    expectThat(testResultChannel.receive()).isA<Success>()
                    expectThat(testResultChannel.receive()).isA<Success>()
                }
            }
        }

        describe("error handling") {
            itWill("fail with duplicate context") {
                val ctx = RootContext {
                    test("duplicate test name") {
                    }
                    test("duplicate test name") {
                    }
                }
                coroutineScope {
                    expectThrows<FailFastException> {
                        ContextExecutor(ctx, testResultChannel, this).execute()
                    }
                }

            }

        }
        it("can close resources") {
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

