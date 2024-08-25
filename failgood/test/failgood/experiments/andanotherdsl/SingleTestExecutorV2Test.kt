package failgood.experiments.andanotherdsl

import failgood.Failure
import failgood.Ignored
import failgood.Success
import failgood.Test
import failgood.TestResult
import failgood.testCollection
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo

@Test
class SingleTestExecutorV2Test {
    val tests =
        testCollection(ignored = Ignored.TODO) {
            val events = mutableListOf<String>()
            describe("test execution") {
                describe("a context without given") {
                    val ctx: TestFunction = {
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

                    it("executes a single test") {
                        val result = executeTest(listOf("root context", "test 1"), ctx)

                        expectThat(events).containsExactly("root context", "test 1")
                        expectThat(result).isA<Success>()
                    }
                    it("executes a nested single test") {
                        val result =
                            executeTest(
                                listOf("root context", "context 1", "context 2", "test 3"), ctx)

                        expectThat(events)
                            .containsExactly("root context", "context 1", "context 2", "test 3")
                        expectThat(result).isA<Success>()
                    }
                }
            }
            describe("error handling") {
                it("reports exceptions in the context as test failures") {
                    val runtimeException = RuntimeException()
                    val contextThatThrows =
                        TestCollection("root context") { throw runtimeException }
                    val result =
                        executeTest(listOf("root context", "test 1"), contextThatThrows.function)

                    expectThat(result).isA<Failure>().get { failure }.isEqualTo(runtimeException)
                }
                it("reports exceptions in the before each function as test failures") {
                    val runtimeException = RuntimeException()
                    val contextThatThrows =
                        TestCollection("root context") {
                            beforeTest { throw runtimeException }
                            test("test") {}
                        }
                    val result =
                        executeTest(listOf("root context", "test 1"), contextThatThrows.function)

                    expectThat(result).isA<Failure>().get { failure }.isEqualTo(runtimeException)
                }
            }
        }
}

@Suppress("UNUSED_PARAMETER")
internal fun executeTest(contextPath: List<String>, ctx: suspend TestDSL.() -> Unit): TestResult {
    return Failure(RuntimeException())
}
