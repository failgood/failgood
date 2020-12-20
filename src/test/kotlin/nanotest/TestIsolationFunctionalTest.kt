package nanotest

import strikt.api.expectThat
import strikt.assertions.containsExactly

object TestIsolationFunctionalTest {
    val context = context {
        test("test isolation") {
            val events = mutableListOf<String>()
            Suite {
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
                "root context", "test 1",
                "root context", "test 2",
                "root context", "context 1", "context 2", "test 3"
            )

        }

    }
}
