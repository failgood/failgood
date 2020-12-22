package failfast

import strikt.api.expectThat
import strikt.assertions.containsExactly

object TestLifecycleTest {
    val context = context {
        xtest("test lifecycle") {
            val events = mutableListOf<String>()
            Suite(1) {
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
            }.run().check()

            expectThat(events).containsExactly(
                "root context", "test 1", "autoclosed",
                "root context", "test 2", "autoclosed",
                "root context", "context 1", "context 2", "test 3", "autoclosed"
            )

        }

    }
}
