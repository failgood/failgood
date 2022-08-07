package failgood.functional

import failgood.Suite
import failgood.Test
import failgood.describe
import java.util.concurrent.CopyOnWriteArrayList

@Test
object SubContextIsolationTest {
    val tests = describe("sub context isolation") {
        describe("on a root context with default isolation (=ON)") {
            val evt = NestedEvents()
            it("can turn isolation off for subcontexts") {
                Suite(
                    describe("root", isolation = true, disabled = false) {
                        val events = evt.addEvent()
                        withoutIsolation {
                            describe("child") {
                                events.add("childContext")
                                it("test1") { events.add("test1") }
                                it("test2") { events.add("test2") }
                            }
                        }
                    }
                ).run(silent = true)
                assert(evt.globalEvents.single().sorted() == listOf("childContext", "test1", "test2").sorted())
            }
            it("does not affect other contexts") {
                Suite(
                    describe("root", isolation = true, disabled = false) {
                        val events = evt.addEvent()
                        withoutIsolation {
                            describe("child") {
                                events.add("childContext")
                                it("test1") { events.add("test1") }
                                it("test2") { events.add("test2") }
                            }
                        }
                        describe("child with isolation") {
                            events.add("child with isolation")
                            it("test3") { events.add("test3") }
                            it("test4") { events.add("test4") }
                        }
                    }
                ).run(silent = true)

                val e = evt.globalEvents
                assert(e.size == 3)
                assert(e[0].sorted() == listOf("childContext", "test1", "test2").sorted())
                assert(e[1] == listOf("child with isolation", "test3"))
                assert(e[2] == listOf("child with isolation", "test4"))
            }
            it("calls callbacks at the correct time") {
                Suite(
                    describe("root", isolation = true, disabled = false) {
                        val events = evt.addEvent()
                        withoutIsolation {
                            describe("child") {
                                afterEach { events.add("no-isolation-afterEach") }
                                autoClose("yo") { events.add("no-isolation-autoClose") }
                                events.add("childContext")
                                it("test1") { events.add("test1") }
                                it("test2") { events.add("test2") }
                            }
                        }
                        describe("child with isolation") {
                            events.add("child with isolation")
                            it("test3") { events.add("test3") }
                            it("test4") { events.add("test4") }
                        }
                    }
                ).run(silent = true)

                val e = evt.globalEvents
                assert(e.size == 3)
                val noIsolationRun = e[0]
                assert(noIsolationRun.take(3).sorted() == listOf("childContext", "test1", "test2").sorted())
                assert(
                    noIsolationRun.takeLast(2).sorted() ==
                        listOf("no-isolation-afterEach", "no-isolation-autoClose")
                )
                assert(e[1] == listOf("child with isolation", "test3"))
                assert(e[2] == listOf("child with isolation", "test4"))
            }
        }
    }
}
private class NestedEvents {
    val globalEvents = CopyOnWriteArrayList<CopyOnWriteArrayList<String>>()
    fun addEvent() = CopyOnWriteArrayList<String>().also { globalEvents.add(it) }
}
