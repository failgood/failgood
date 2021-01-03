package failfast

import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder

fun main() {
    Suite(TestLifecycleTest.context).run().check(false)
}

object TestLifecycleTest {
    const val ROOT_CONTEXT_EXECUTED = "root context executed"
    const val DEPENDENCY_CLOSED = "autoClose callback called"
    const val TEST_1_EXECUTED = "test 1 executed"
    const val TEST_2_EXECUTED = "test 2 executed"
    const val CONTEXT_1_EXECUTED = "context 1 executed"
    const val CONTEXT_2_EXECUTED = "context 2"
    const val TEST_3_EXECUTED = "test 3 executed"
    const val TEST_4_EXECUTED = "test 4 executed"
    val context =
        describe("test dependencies") {
            it("are recreated for each test") {

                // the total order of events is not defined because tests run in parallel.
                // so we track events in a list of a list and record the events that lead to each test execution
                val totalEvents = mutableListOf<List<String>>()
                Suite {
                    val testEvents = mutableListOf<String>()
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
                        listOf(ROOT_CONTEXT_EXECUTED, CONTEXT_1_EXECUTED, CONTEXT_2_EXECUTED, TEST_3_EXECUTED, DEPENDENCY_CLOSED),
                        listOf(ROOT_CONTEXT_EXECUTED, TEST_4_EXECUTED, DEPENDENCY_CLOSED)
                    )
            }
        }
}
