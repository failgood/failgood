package failgood.functional

import failgood.Suite
import failgood.Test
import failgood.assert.containsExactlyInAnyOrder
import failgood.assert.endsWith
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
                assert(evt.globalEvents.single().containsExactlyInAnyOrder(listOf("childContext", "test1", "test2")))
            }
            it("does not affect other contexts") {
                assert(
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
                    ).run(silent = true).allOk
                )

                val e = evt.globalEvents
                assert(e.size == 3)
                assert(e[0].containsExactlyInAnyOrder("childContext", "test1", "test2"))
                assert(e[1] == listOf("child with isolation", "test3"))
                assert(e[2] == listOf("child with isolation", "test4"))
            }
            it("calls callbacks at the correct time") {
                assert(
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
                    ).run(silent = true).allOk
                )

                val e = evt.globalEvents
                assert(e.size == 3)
                val noIsolationRun = e[0]
                assert(noIsolationRun.first() == "childContext")
                assert(
                    noIsolationRun.containsExactlyInAnyOrder(
                        "childContext",
                        "test1",
                        "no-isolation-afterEach",
                        "test2",
                        "no-isolation-afterEach",
                        "no-isolation-autoClose"
                    )
                )
                assert(
                    noIsolationRun.endsWith("no-isolation-afterEach", "no-isolation-autoClose")
                )
                assert(e[1] == listOf("child with isolation", "test3"))
                assert(e[2] == listOf("child with isolation", "test4"))
            }
        }
    }
}

class NestedEvents {
    val globalEvents = CopyOnWriteArrayList<CopyOnWriteArrayList<String>>()
    fun addEvent() = CopyOnWriteArrayList<String>().also { globalEvents.add(it) }
}
