package failgood.internal

import failgood.*
import failgood.dsl.ContextLambda
import failgood.dsl.TestDSLWithGiven
import failgood.mock.mock
import kotlinx.coroutines.coroutineScope
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo

@Test
class SingleTestExecutorTest {
    val context =
        describe(SingleTestExecutor::class) {
            val testDSL = mock<TestDSLWithGiven<Unit>>()
            val resourceCloser = coroutineScope { ResourceCloserImpl(this) }
            describe("test execution") {
                val events = mutableListOf<String>()
                val ctx: ContextLambda = {
                    events.add("root context")
                    test("test 1") { events.add("test 1") }
                    test("test 2") { events.add("test 2") }
                    context("context 1") {
                        events.add("context 1")

                        context("context 2") {
                            events.add("context 2")
                            test("test 3") { events.add("test 3") }
                        }
                    }
                }
                val rootContext = Context("root context", null)
                val context1 = Context("context 1", rootContext)
                val context2 = Context("context 2", context1)

                it("executes a single test") {
                    val result =
                        SingleTestExecutor(
                                ContextPath(rootContext, "test 1"),
                                testDSL,
                                resourceCloser,
                                ctx
                            )
                            .execute()
                    expectThat(events).containsExactly("root context", "test 1")
                    expectThat(result).isA<Success>()
                }
                it("executes a nested single test") {
                    val result =
                        SingleTestExecutor(
                                ContextPath(context2, "test 3"),
                                testDSL,
                                resourceCloser,
                                ctx
                            )
                            .execute()
                    expectThat(events)
                        .containsExactly("root context", "context 1", "context 2", "test 3")
                    expectThat(result).isA<Success>()
                }
            }
            it("also supports describe / it") {
                val context: ContextLambda = {
                    describe("with a valid root context") {
                        it("returns number of tests") {}
                        it("returns contexts") {}
                    }
                }
                val test =
                    ContextPath(
                        Context("with a valid root context", Context("ContextExecutor", null)),
                        "returns contexts"
                    )
                val executor = SingleTestExecutor(test, testDSL, resourceCloser, context)
                executor.execute()
            }
            describe("error handling") {
                it("reports exceptions in the context as test failures") {
                    val runtimeException = RuntimeException()
                    val contextThatThrows = RootContext("root context") { throw runtimeException }
                    val result =
                        SingleTestExecutor(
                                ContextPath(Context("root context", null), "test"),
                                testDSL,
                                resourceCloser,
                                contextThatThrows.function
                            )
                            .execute()
                    expectThat(result).isA<Failure>().get { failure }.isEqualTo(runtimeException)
                }
                it("reports exceptions in the autoclose lambda as test failures") {
                    val runtimeException = RuntimeException()
                    val contextThatThrows =
                        RootContext("root context") {
                            autoClose("String") { throw runtimeException }
                            it("test") {}
                        }
                    val result =
                        SingleTestExecutor(
                                ContextPath(Context("root context", null), "test"),
                                testDSL,
                                resourceCloser,
                                contextThatThrows.function
                            )
                            .execute()
                    expectThat(result).isA<Failure>().get { failure }.isEqualTo(runtimeException)
                }
            }
        }
}
