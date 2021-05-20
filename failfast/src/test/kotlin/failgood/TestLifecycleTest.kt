package failgood

import failgood.TestLifecycleTest.Event.*
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import java.util.concurrent.CopyOnWriteArrayList


@Testable
class TestLifecycleTest {
    private enum class Event {
        ROOT_CONTEXT_EXECUTED,
        DEPENDENCY_CLOSED,
        TEST_1_EXECUTED,
        TEST_2_EXECUTED,
        CONTEXT_1_EXECUTED,
        CONTEXT_2_EXECUTED,
        TEST_3_EXECUTED,
        TEST_4_EXECUTED
    }

    val context = describe("test dependencies") {
        it("are recreated for each test") {
            // tests run in parallel, so the total order of events is not defined.
            // we track events in a list of lists and record the events that lead to each test
            val totalEvents = CopyOnWriteArrayList<List<Event>>()
            Suite {
                val testEvents = mutableListOf<Event>()
                totalEvents.add(testEvents)
                testEvents.add(ROOT_CONTEXT_EXECUTED)
                autoClose("dependency", closeFunction = { testEvents.add(DEPENDENCY_CLOSED) })
                test("test 1") { testEvents.add(TEST_1_EXECUTED) }
                test("test 2") { testEvents.add(TEST_2_EXECUTED) }
                context("context 1") {
                    testEvents.add(CONTEXT_1_EXECUTED)

                    context("context 2") {
                        testEvents.add(CONTEXT_2_EXECUTED)
                        test("test 3") { testEvents.add(TEST_3_EXECUTED) }
                    }
                }
                test("test4: tests can be defined after contexts") {
                    testEvents.add(TEST_4_EXECUTED)
                }
            }.run()

            expectThat(totalEvents)
                .containsExactlyInAnyOrder(
                    listOf(ROOT_CONTEXT_EXECUTED, TEST_1_EXECUTED, DEPENDENCY_CLOSED),
                    listOf(ROOT_CONTEXT_EXECUTED, TEST_2_EXECUTED, DEPENDENCY_CLOSED),
                    listOf(
                        ROOT_CONTEXT_EXECUTED,
                        CONTEXT_1_EXECUTED,
                        CONTEXT_2_EXECUTED,
                        TEST_3_EXECUTED,
                        DEPENDENCY_CLOSED
                    ),
                    listOf(ROOT_CONTEXT_EXECUTED, TEST_4_EXECUTED, DEPENDENCY_CLOSED)
                )
        }
    }
}
