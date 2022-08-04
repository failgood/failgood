package failgood.functional

import failgood.Suite
import failgood.Test
import failgood.describe
import java.util.concurrent.CopyOnWriteArrayList

@Test
object SubContextIsolationTest {
    val tests = describe("sub context isolation") {
        describe("on a root context with default isolation (=ON)") {
            it("can turn isolation off for subcontexts") {
                val events = CopyOnWriteArrayList<String>()
                Suite(
                    describe("root", isolation = true, disabled = false) {
                        withoutIsolation {
                            describe("child") {
                                events.add("childContext")
                                it("test1") { events.add("test1") }
                                it("test2") { events.add("test2") }
                            }
                        }
                    }
                ).run(silent = true)
                assert(events == listOf("childContext", "test1", "test2"))
            }
        }
    }
}
