package failgood.functional

import failgood.Suite
import failgood.Test
import failgood.describe
import java.util.concurrent.CopyOnWriteArrayList

@Test
object SubContextIsolationTest {
    val tests = describe("sub context isolation") {
        describe("on a root context with default isolation (=ON)") {
            val globalEvents = CopyOnWriteArrayList<CopyOnWriteArrayList<String>>()
            it("can turn isolation off for subcontexts") {
                Suite(
                    describe("root", isolation = true, disabled = false) {
                        val events = CopyOnWriteArrayList<String>()
                        globalEvents.add(events)
                        withoutIsolation {
                            describe("child") {
                                events.add("childContext")
                                it("test1") { events.add("test1") }
                                it("test2") { events.add("test2") }
                            }
                        }
                    }
                ).run(silent = true)
                assert(globalEvents.single() == listOf("childContext", "test1", "test2"))
            }
            it("does not affect other contests") {
                Suite(
                    describe("root", isolation = true, disabled = false) {
                        val events = CopyOnWriteArrayList<String>()
                        globalEvents.add(events)
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
                assert(globalEvents.size == 3)
                assert(globalEvents[0] == listOf("childContext", "test1", "test2"))
                assert(globalEvents[1] == listOf("child with isolation", "test3"))
                assert(globalEvents[2] == listOf("child with isolation", "test4"))
            }
        }
    }
}
