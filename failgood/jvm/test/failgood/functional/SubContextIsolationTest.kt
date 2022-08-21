package failgood.functional

import failgood.Suite
import failgood.Test
import failgood.assert.containsExactlyInAnyOrder
import failgood.assert.endsWith
import failgood.describe
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertNotNull

@Test
object SubContextIsolationTest {
    val tests = describe("sub context isolation") {
        describe("on a root context with default isolation (true)") {
            val evt = NestedEvents()
            describe("turning off isolation for a subcotext") {
                it("executes all the tests in that context in one go") {
                    Suite(
                        failgood.describe("root") {
                            val events = evt.addEvent()
                            describe("child", isolation = false) {
                                events.add("childContext")
                                it("test1") { events.add("test1") }
                                it("test2") { events.add("test2") }
                            }
                        }
                    ).run(silent = true)
                    val singleEvent = assertNotNull(evt.globalEvents.singleOrNull())
                    assert(singleEvent.containsExactlyInAnyOrder(listOf("childContext", "test1", "test2")))
                }
                it("does not affect other contexts") {
                    assert(
                        Suite(
                            failgood.describe("root") {
                                val events = evt.addEvent()
                                describe("child", isolation = false) {
                                    events.add("childContext")
                                    it("test1") { events.add("test1") }
                                    it("test2") { events.add("test2") }
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
                it("calls the given for each test") {
                    var givenCalls = 0
                    val givenValues = ConcurrentLinkedQueue<Int>()
                    Suite(
                        failgood.describe("root") {
                            val events = evt.addEvent()
                            describe("child", isolation = false, given = { givenCalls++ }) {
                                events.add("childContext")
                                it("test1") { given ->
                                    events.add("test1")
                                    givenValues.add(given)
                                }
                                it("test2") { given ->
                                    events.add("test2")
                                    givenValues.add(given)
                                }
                            }
                        }
                    ).run(silent = true)
                    val singleEvent = assertNotNull(evt.globalEvents.singleOrNull())
                    assert(singleEvent.containsExactlyInAnyOrder(listOf("childContext", "test1", "test2")))
                    assert(givenValues.containsExactlyInAnyOrder(listOf(0, 1)))
                    assert(givenCalls == 2)
                }
                it("calls callbacks at the correct time") {
                    assert(
                        Suite(
                            failgood.describe("root") {
                                val events = evt.addEvent()
                                describe("child", isolation = false) {
                                    afterEach { events.add("no-isolation-afterEach") }
                                    autoClose("yo") { events.add("no-isolation-autoClose") }
                                    events.add("childContext")
                                    it("test1") { events.add("test1") }
                                    it("test2") { events.add("test2") }
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
            describe("error handling") {
                val e = NestedEvents()
                it("does not allow to turn isolation on when it was already off") {
                    val result = Suite(
                        failgood.describe("root context without isolation", isolation = false) {
                            describe("sub context that tries to turn isolation on", isolation = true) {
                                test("this test should never run") {
                                    e.addEvent()
                                }
                            }
                        }
                    ).run(silent = true)
                    val failedContext = assertNotNull(result.failedTests.singleOrNull()?.test)
                    assert(
                        failedContext.testName == "error in context" &&
                            failedContext.container.name == "sub context that tries to turn isolation on"
                    )
                    assert(e.globalEvents.isEmpty())
                }
            }
        }
    }
}

class NestedEvents {
    val globalEvents = CopyOnWriteArrayList<CopyOnWriteArrayList<String>>()
    fun addEvent() = CopyOnWriteArrayList<String>().also { globalEvents.add(it) }
}
