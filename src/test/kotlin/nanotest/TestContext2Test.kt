package nanotest

import nanotest.exp.TestContext2
import strikt.api.expectThat
import strikt.assertions.containsExactly

object TestContext2Test {
    val context = Context("fresh context for every test") {
        xtest("runs first test and collects all tests") {
            val events = mutableListOf<String>()
            val ctx = Context("the context") {
                events.add("root context")
                test("test 1") {
                    events.add("test 1")
                }
                test("test 2") {
                    events.add("test 2")
                }
            }
            TestContext2(ctx).execute()
            expectThat(events).containsExactly("root context", "test 1")
        }
    }

}
