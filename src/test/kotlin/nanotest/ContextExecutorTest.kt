package nanotest

import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.isA

object ContextExecutorTest {
    val context = Context {
        test("context executor test") {
            val events = mutableListOf<String>()
            val ctx = Context("root context") {
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

            }
            val result: List<TestResult> = ContextExecutor(ctx).execute()
            expectThat(events).containsExactly(
                "root context", "test 1",
                "root context", "test 2",
                "root context", "context 1", "context 2", "test 3"
            )
            expectThat(result).all { isA<Success>() }

        }
    }
}

