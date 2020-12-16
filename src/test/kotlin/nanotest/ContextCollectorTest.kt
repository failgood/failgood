package nanotest

import nanotest.exp.ContextCollector
import nanotest.exp.ContextInfo
import nanotest.exp.TestDescriptor
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder

object ContextCollectorTest {
    val context = Context {
        test("runs first test and collects all tests") {
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
            val tests: ContextInfo = ContextCollector(ctx).execute()
            expectThat(tests.tests).containsExactlyInAnyOrder(
                TestDescriptor(listOf(), "test 1"),
                TestDescriptor(listOf(), "test 2"),
                TestDescriptor(listOf("context 1", "context 2"), "test 3")
            )

            // TODO: here one test should run (async) to use the created contexts.
            // and closeables need to be closed to make sure we don't leak
            expectThat(events).containsExactly("root context", "context 1", "context 2")
        }
    }

}
