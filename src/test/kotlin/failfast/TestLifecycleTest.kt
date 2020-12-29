package failfast

import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder

fun main() {
    Suite(TestLifecycleTest.context).run().check(false)
}

object TestLifecycleTest {
    val context = context {
        test("test lifecycle") {
            val totalEvents = mutableListOf<List<String>>()
            Suite(1) {
                val events = mutableListOf<String>()
                totalEvents.add(events)
                autoClose("nothing", { events.add("autoclosed") })
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
                test("tests can be defined after contexts") {
                    events.add("tests can be defined after contexts")
                }
            }.run()

            expectThat(totalEvents).containsExactlyInAnyOrder(
                listOf("root context", "test 1", "autoclosed"),
                listOf("root context", "test 2", "autoclosed"),
                listOf("root context", "context 1", "context 2", "test 3", "autoclosed"),
                listOf("root context", "tests can be defined after contexts", "autoclosed")
            )

        }

    }
}
