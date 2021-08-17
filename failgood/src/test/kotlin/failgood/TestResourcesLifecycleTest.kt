package failgood

import failgood.mock.call
import failgood.mock.getCalls
import failgood.mock.mock
import failgood.mock.verify
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isSameInstanceAs
import strikt.assertions.isTrue
import java.util.concurrent.CopyOnWriteArrayList

@Test
class TestResourcesLifecycleTest {
    val context = describe("closing test resources") {
        it("closes resources in reverse order of creation") {
            val closeable1 = mock<AutoCloseable>()
            val closeable2 = mock<AutoCloseable>()
            var resource1: AutoCloseable? = null
            var resource2: AutoCloseable? = null
            val totalEvents = CopyOnWriteArrayList<List<String>>()
            expectThat(Suite {
                val events = mutableListOf<String>()
                totalEvents.add(events)
                resource1 = autoClose(closeable1) { it.close(); events.add("first close callback") }
                resource2 = autoClose(closeable2) { it.close(); events.add("second close callback") }
                test("first  test") { events.add("first test") }
                test("second test") { events.add("second test") }
            }.run(silent = true)).get { allOk }.isTrue()
            expectThat(totalEvents).containsExactly(
                listOf("first test", "second close callback", "first close callback"),
                listOf("second test", "second close callback", "first close callback"),
            )
            expectThat(resource1).isSameInstanceAs(closeable1)
            expectThat(resource2).isSameInstanceAs(closeable2)
            expectThat(getCalls(closeable1)).containsExactly(call(AutoCloseable::close), call(AutoCloseable::close))
            expectThat(getCalls(closeable2)).containsExactly(call(AutoCloseable::close), call(AutoCloseable::close))
            verify(closeable1) { close() }
            verify(closeable2) { close() }
        }
        it("closes autocloseables without callback") {
            var ac1: AutoCloseable? = null
            var ac2: AutoCloseable? = null
            var resource1: AutoCloseable? = null
            var resource2: AutoCloseable? = null
            val totalEvents = CopyOnWriteArrayList<List<String>>()
            expectThat(Suite {
                val events = mutableListOf<String>()
                totalEvents.add(events)
                ac1 = AutoCloseable { events.add("first close callback") }
                resource1 = autoClose(ac1!!)
                ac2 = AutoCloseable { events.add("second close callback") }
                resource2 = autoClose(ac2!!)
                test("first test") { events.add("first test") }
                test("second test") { events.add("second test") }
            }.run(silent = true)).get { allOk }.isTrue()
            expectThat(totalEvents).containsExactly(
                listOf("first test", "second close callback", "first close callback"),
                listOf("second test", "second close callback", "first close callback"),
            )
            expectThat(resource1).isSameInstanceAs(ac1)
            expectThat(resource2).isSameInstanceAs(ac2)
        }
        it("autocloseable works inside a test") {
            val closeable1 = mock<AutoCloseable>()
            val closeable2 = mock<AutoCloseable>()
            var resource1: AutoCloseable? = null
            var resource2: AutoCloseable? = null
            val totalEvents = CopyOnWriteArrayList<List<String>>()
            expectThat(Suite {
                val events = mutableListOf<String>()
                totalEvents.add(events)
                test("first  test") {
                    events.add("first test")
                    resource1 = autoClose(closeable1) { it.close(); events.add("first close callback") }
                }
                test("second test") {
                    events.add("second test")
                    resource2 = autoClose(closeable2) { it.close(); events.add("second close callback") }
                }
            }.run(silent = true)).get { allOk }.isTrue()
            expectThat(totalEvents).containsExactly(
                listOf("first test", "first close callback"),
                listOf("second test", "second close callback"),
            )
            expectThat(resource1).isSameInstanceAs(closeable1)
            expectThat(resource2).isSameInstanceAs(closeable2)
            expectThat(getCalls(closeable1)).containsExactly(call(AutoCloseable::close))
            expectThat(getCalls(closeable2)).containsExactly(call(AutoCloseable::close))
            verify(closeable1) { close() }
            verify(closeable2) { close() }
        }
        describe("after suite callback") {
            it("is called exactly once at the end of the suite, after all tests are finished") {
                val events = CopyOnWriteArrayList<String>()
                expectThat(Suite {
                    afterSuite {
                        events.add("afterSuite callback")
                    }
                    test("first  test") {
                        events.add("first test")
                    }
                    // this subcontext is here to make sure the context executor has to execute the dsl twice
                    // to test that this does not create duplicate after suite callbacks.
                    context("sub context") {
                        afterSuite {
                            events.add("afterSuite callback in subcontext")
                        }

                        test("sub context test") {
                            events.add("sub context test")
                        }
                        context("another subcontext") {
                            test("with a test") {}
                        }
                    }
                    test("second test") {
                        events.add("second test")
                    }
                }.run(silent = true)).get { allOk }.isTrue()
                expectThat(events.takeLast(2)).containsExactlyInAnyOrder(
                    "afterSuite callback",
                    "afterSuite callback in subcontext"
                )
                expectThat(events).containsExactlyInAnyOrder(
                    "first test",
                    "second test",
                    "sub context test",
                    "afterSuite callback",
                    "afterSuite callback in subcontext"

                )
            }
            it("can throw exceptions that are ignored") {
                val events = CopyOnWriteArrayList<String>()
                expectThat(Suite {
                    afterSuite {
                        events.add("afterSuite callback")
                    }
                    afterSuite {
                        throw AssertionError()
                    }
                    afterSuite {
                        throw RuntimeException()
                    }
                }.run(silent = true)).get { allOk }.isTrue()
                expectThat(events).containsExactly("afterSuite callback")
            }
        }
    }
}
