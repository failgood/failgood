package failgood.functional

import failgood.ContextDSL
import failgood.Failed
import failgood.Success
import failgood.Suite
import failgood.Test
import failgood.TestResult
import failgood.describe
import failgood.functional.TestLifecycleTest.Event.*
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
class TestLifecycleTest {
    private enum class Event {
        ROOT_CONTEXT_EXECUTED,
        DEPENDENCY_CLOSED,
        TEST_1_EXECUTED,
        TEST_2_EXECUTED,
        CONTEXT_1_EXECUTED,
        CONTEXT_2_EXECUTED,
        TEST_3_EXECUTED,
        TEST_4_EXECUTED,
        AFTER_EACH_EXECUTED
    }

    val context = describe("the test lifecycle") {
        val afterEachParameters = ConcurrentHashMap<String, TestResult>()
        val totalEvents = CopyOnWriteArrayList<List<Event>>()
        suspend fun ContextDSL<Unit>.contextFixture() {
            val testEvents = CopyOnWriteArrayList<Event>()
            totalEvents.add(testEvents)
            testEvents.add(ROOT_CONTEXT_EXECUTED)
            autoClose("dependency", closeFunction = { testEvents.add(DEPENDENCY_CLOSED) })
            afterEach { result: TestResult ->
                afterEachParameters[testInfo.name] = result
                testEvents.add(AFTER_EACH_EXECUTED)
            }
            test("test 1") { testEvents.add(TEST_1_EXECUTED) }
            test("test 2") { testEvents.add(TEST_2_EXECUTED) }
            context("context 1") {
                testEvents.add(CONTEXT_1_EXECUTED)
                context("context 2") {
                    testEvents.add(CONTEXT_2_EXECUTED)
                    test("test 3") {
                        testEvents.add(TEST_3_EXECUTED)
                        throw RuntimeException()
                    }
                }
            }
            test("test4: tests can be defined after contexts") {
                testEvents.add(TEST_4_EXECUTED)
            }
        }

        suspend fun ContextDSL<Unit>.testAfterEach() {
            it("passes testInfo and success to afterEach") {
                expectThat(afterEachParameters.keys().toList()).containsExactlyInAnyOrder(
                    "test 1",
                    "test 2",
                    "test 3",
                    "test4: tests can be defined after contexts"
                )
                assert(afterEachParameters["test 1"] is Success)
                assert(afterEachParameters["test 2"] is Success)
                assert(afterEachParameters["test 3"] is Failed)
                assert(afterEachParameters["test4: tests can be defined after contexts"] is Success)
            }
        }
        describe("in the default isolation mode (full isolation)") {
            val result = Suite {
                contextFixture()
            }.run(silent = true)
            assert(!result.allOk) { "there should be one failing test" }
            it("test dependencies are recreated for each test") {
                // tests run in parallel, so the total order of events is not defined.
                // we track events in a list of lists and record the events that lead to each test
                expectThat(totalEvents)
                    .containsExactlyInAnyOrder(
                        listOf(ROOT_CONTEXT_EXECUTED, TEST_1_EXECUTED, DEPENDENCY_CLOSED, AFTER_EACH_EXECUTED),
                        listOf(ROOT_CONTEXT_EXECUTED, TEST_2_EXECUTED, DEPENDENCY_CLOSED, AFTER_EACH_EXECUTED),
                        listOf(
                            ROOT_CONTEXT_EXECUTED,
                            CONTEXT_1_EXECUTED,
                            CONTEXT_2_EXECUTED,
                            TEST_3_EXECUTED,
                            DEPENDENCY_CLOSED,
                            AFTER_EACH_EXECUTED
                        ),
                        listOf(ROOT_CONTEXT_EXECUTED, TEST_4_EXECUTED, DEPENDENCY_CLOSED, AFTER_EACH_EXECUTED)
                    )
            }
            testAfterEach()
        }
        describe("a root context with isolation set to false") {
            Suite(
                describe("root context without isolation", isolation = false) {
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
                            TEST_1_EXECUTED,
                            TEST_2_EXECUTED,
                            CONTEXT_1_EXECUTED,
                            CONTEXT_2_EXECUTED,
                            TEST_3_EXECUTED,
                            TEST_4_EXECUTED,
                            DEPENDENCY_CLOSED,
                            AFTER_EACH_EXECUTED, // after each is executed once per test even when isolation == false
                            AFTER_EACH_EXECUTED,
                            AFTER_EACH_EXECUTED,
                            AFTER_EACH_EXECUTED
                        )
                    )
                    first().isEqualTo(ROOT_CONTEXT_EXECUTED)
                    last().isEqualTo(DEPENDENCY_CLOSED)
                    // assert that tests run after the contexts that they are in.
                    get { intersect(setOf(CONTEXT_1_EXECUTED, CONTEXT_2_EXECUTED, TEST_3_EXECUTED)) }.containsExactly(
                        listOf(CONTEXT_1_EXECUTED, CONTEXT_2_EXECUTED, TEST_3_EXECUTED)
                    )
                    get { intersect(setOf(CONTEXT_1_EXECUTED, CONTEXT_2_EXECUTED, TEST_4_EXECUTED)) }.containsExactly(
                        listOf(CONTEXT_1_EXECUTED, CONTEXT_2_EXECUTED, TEST_4_EXECUTED)
                    )
                }
            }
            testAfterEach()
        }
    }
}
