package failgood.functional

import failgood.*
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.first
import strikt.assertions.isEqualTo
import strikt.assertions.last
import strikt.assertions.single
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Test
class RootContextTestIsolationTest {
    companion object {
        const val ROOT_CONTEXT_EXECUTED = "root context executed"
        const val DEPENDENCY_CLOSED = "dependency closed"
        const val CONTEXT_1_EXECUTED = "context 1 executed"
        const val CONTEXT_2_EXECUTED = "context 2 executed"
    }

    val context = describe("test isolation for root contexts") {
        val afterEachParameters = ConcurrentHashMap<String, TestResult>()
        val totalEvents = CopyOnWriteArrayList<List<String>>()
        suspend fun ContextDSL<Unit>.contextFixture() {
            val testEvents = CopyOnWriteArrayList<String>()
            totalEvents.add(testEvents)
            testEvents.add(ROOT_CONTEXT_EXECUTED)
            autoClose("dependency", closeFunction = {
                testEvents.add(DEPENDENCY_CLOSED)
            })
            afterEach { result: TestResult ->
                val testName = testInfo.name
                afterEachParameters[testName] = result
                testEvents.add("after each executed for $testName")
            }
            val logTest: suspend TestDSL.(Unit) -> Unit = { testEvents.add("${testInfo.name} executed") }
            test("test 1", function = logTest)
            test("test 2", function = logTest)
            context("context 1") {
                testEvents.add(CONTEXT_1_EXECUTED)
                context("context 2") {
                    testEvents.add(CONTEXT_2_EXECUTED)
                    test("test 3") {
                        testEvents.add("${testInfo.name} executed")
                        throw RuntimeException()
                    }
                }
            }
            // tests can be defined after contexts
            test("test 4", function = logTest)
        }

        suspend fun ContextDSL<Unit>.testAfterEach() {
            it("passes testInfo and success to afterEach") {
                expectThat(afterEachParameters.keys().toList()).containsExactlyInAnyOrder(
                    "test 1", "test 2", "test 3", "test 4"
                )
                assert(afterEachParameters["test 1"] is Success)
                assert(afterEachParameters["test 2"] is Success)
                assert(afterEachParameters["test 3"] is Failure)
                assert(afterEachParameters["test 4"] is Success)
            }
        }
        describe("in the default isolation mode (full isolation)") {
            val result = Suite {
                contextFixture()
            }.run(silent = true)
            it("test dependencies are recreated for each test") {
                assert(!result.allOk) { "there should be one failing test" }
                // tests run in parallel, so the total order of events is not defined.
                // we track events in a list of lists and record the events that lead to each test
                expectThat(totalEvents).containsExactlyInAnyOrder(
                    listOf(
                        ROOT_CONTEXT_EXECUTED, "test 1 executed", "after each executed for test 1", DEPENDENCY_CLOSED
                    ),
                    listOf(
                        ROOT_CONTEXT_EXECUTED, "test 2 executed", "after each executed for test 2", DEPENDENCY_CLOSED
                    ),
                    listOf(
                        ROOT_CONTEXT_EXECUTED,
                        CONTEXT_1_EXECUTED,
                        CONTEXT_2_EXECUTED,
                        "test 3 executed",
                        "after each executed for test 3",
                        DEPENDENCY_CLOSED
                    ),
                    listOf(
                        ROOT_CONTEXT_EXECUTED, "test 4 executed", "after each executed for test 4", DEPENDENCY_CLOSED
                    )
                )
            }
            testAfterEach()
        }
        describe("a root context with isolation set to false") {
            Suite(
                failgood.describe("root context without isolation", isolation = false) {
                    contextFixture()
                }
            ).run(silent = true)
            it("runs tests without recreating the dependencies") {
                // here we just know that the root context start is the first event and the resource closed the last
                // other events can occur in any order

                // we don't know the order of the tests because they run in parallel
                // we do know that the root context runs first and the dependency must be closed after all tests are finished
                expectThat(totalEvents).single().and {
                    containsExactlyInAnyOrder(
                        listOf(
                            ROOT_CONTEXT_EXECUTED,
                            "test 1 executed",
                            "test 2 executed",
                            "test 3 executed",
                            "test 4 executed",
                            CONTEXT_1_EXECUTED,
                            CONTEXT_2_EXECUTED,
                            DEPENDENCY_CLOSED,
                            "after each executed for test 1",
                            "after each executed for test 2",
                            "after each executed for test 3",
                            "after each executed for test 4"
                        )
                    )
                    first().isEqualTo(ROOT_CONTEXT_EXECUTED)
                    last().isEqualTo(DEPENDENCY_CLOSED)
                    containsInOrder(listOf("test 1 executed", "after each executed for test 1", DEPENDENCY_CLOSED))
                    containsInOrder(listOf("test 2 executed", "after each executed for test 2", DEPENDENCY_CLOSED))
                    // assert that tests run after the contexts that they are in, and that close callbacks have the correct order
                    containsInOrder(
                        listOf(
                            CONTEXT_1_EXECUTED,
                            CONTEXT_2_EXECUTED,
                            "test 3 executed",
                            "after each executed for test 3",
                            DEPENDENCY_CLOSED
                        )
                    )
                    containsInOrder(
                        listOf(
                            CONTEXT_1_EXECUTED,
                            CONTEXT_2_EXECUTED,
                            "test 4 executed",
                            "after each executed for test 4",
                            DEPENDENCY_CLOSED
                        )
                    )
                }
            }
            testAfterEach()
        }
    }

    /**
     * assert that a list contains these elements in their order
     */
    private fun Assertion.Builder<List<String>>.containsInOrder(elements: List<String>) {
        get { intersect(elements.toSet()) }.containsExactly(elements)
    }
}
