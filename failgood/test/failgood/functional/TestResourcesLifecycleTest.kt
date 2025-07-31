package failgood.functional

import failgood.Failure
import failgood.Suite
import failgood.SuiteResult
import failgood.SuspendAutoCloseable
import failgood.Test
import failgood.mock.call
import failgood.mock.getCalls
import failgood.mock.mock
import failgood.mock.verify
import failgood.softly.softly
import failgood.testCollection
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@Test
class TestResourcesLifecycleTest {
    val tests =
        testCollection("closing test resources") {
            describe("autoclosable") {
                it("is closed in reverse order of creation") {
                    val closeable1 = mock<AutoCloseable>()
                    val closeable2 = mock<AutoCloseable>()
                    var resource1: AutoCloseable? = null
                    var resource2: AutoCloseable? = null
                    val totalEvents = CopyOnWriteArrayList<List<String>>()
                    val result =
                        Suite {
                                val events = mutableListOf<String>()
                                totalEvents.add(events)
                                resource1 =
                                    autoClose(closeable1) {
                                        it.close()
                                        events.add("first close callback")
                                    }
                                resource2 =
                                    autoClose(closeable2) {
                                        it.close()
                                        events.add("second close callback")
                                    }
                                test("first failing test") {
                                    events.add(testInfo.name)
                                    throw AssertionError("test failed")
                                }
                                test("second failing test") {
                                    events.add(testInfo.name)
                                    throw AssertionError("test failed")
                                }
                                test("first test") { events.add(testInfo.name) }
                                test("second test") { events.add(testInfo.name) }
                            }
                            .run(silent = true)
                    assert(!result.allOk)
                    val expectedEvents =
                        setOf(
                            listOf("first test", "second close callback", "first close callback"),
                            listOf("second test", "second close callback", "first close callback"),
                            listOf(
                                "first failing test",
                                "second close callback",
                                "first close callback"),
                            listOf(
                                "second failing test",
                                "second close callback",
                                "first close callback"))
                    assert(totalEvents.toSet() == expectedEvents)
                    assert(resource1 === closeable1)
                    assert(resource2 === closeable2)
                    assert(getCalls(closeable1) == List(4) { call(AutoCloseable::close) })
                    assert(getCalls(closeable2) == List(4) { call(AutoCloseable::close) })
                }
                it("closes autocloseables without callback") {
                    var ac1: AutoCloseable? = null
                    var ac2: SuspendAutoCloseable? = null
                    var resource1: AutoCloseable? = null
                    var resource2: SuspendAutoCloseable? = null
                    val totalEvents = CopyOnWriteArrayList<List<String>>()
                    val result =
                        Suite {
                                val events = mutableListOf<String>()
                                totalEvents.add(events)
                                ac1 = AutoCloseable { events.add("first close callback") }
                                resource1 = autoClose(ac1!!)
                                ac2 = SuspendAutoCloseable { events.add("second close callback") }
                                resource2 = autoClose(ac2!!)
                                test("first test") { events.add("first test") }
                                test("second test") { events.add("second test") }
                            }
                            .run(silent = true)
                    assert(result.allOk)
                    assert(
                        totalEvents ==
                            listOf(
                                listOf(
                                    "first test", "second close callback", "first close callback"),
                                listOf(
                                    "second test",
                                    "second close callback",
                                    "first close callback")))
                    assert(resource1 === ac1)
                    assert(resource2 === ac2)
                }
                it("works inside a test") {
                    val closeable1 = mock<AutoCloseable>()
                    val closeable2 = mock<AutoCloseable>()
                    var resource1: AutoCloseable? = null
                    var resource2: AutoCloseable? = null
                    val totalEvents = CopyOnWriteArrayList<List<String>>()
                    val result =
                        Suite {
                                val events = mutableListOf<String>()
                                totalEvents.add(events)
                                test("first  test") {
                                    events.add("first test")
                                    resource1 =
                                        autoClose(closeable1) {
                                            it.close()
                                            events.add("first close callback")
                                        }
                                }
                                test("second test") {
                                    events.add("second test")
                                    resource2 =
                                        autoClose(closeable2) {
                                            it.close()
                                            events.add("second close callback")
                                        }
                                }
                            }
                            .run(silent = true)
                    assert(result.allOk)
                    assert(
                        totalEvents ==
                            listOf(
                                listOf("first test", "first close callback"),
                                listOf("second test", "second close callback")))
                    assert(resource1 === closeable1)
                    assert(resource2 === closeable2)
                    assert(getCalls(closeable1) == listOf(call(AutoCloseable::close)))
                    assert(getCalls(closeable2) == listOf(call(AutoCloseable::close)))
                    verify(closeable1) { close() }
                    verify(closeable2) { close() }
                }
                describe("error handling") {
                    fun assertFailedGracefully(result: SuiteResult) {
                        assert(!result.allOk)
                        assert(result.allTests.size == 2)
                        assert(result.allTests.all { it.result is Failure })
                        val testNames = result.allTests.map { it.test.testName }.toSet()
                        assert(testNames == setOf("first test", "second test"))
                    }

                    val afterEachCalled = AtomicInteger(0)
                    val autoCloseCalled = AtomicInteger(0)
                    fun suiteResult(
                        testsFail: Boolean,
                        afterEachFails: Boolean,
                        autoCloseFails: Boolean
                    ) =
                        Suite {
                                autoClose(null) {
                                    autoCloseCalled.incrementAndGet()
                                    if (autoCloseFails)
                                        throw RuntimeException("autoclose error message")
                                }
                                afterEach {
                                    afterEachCalled.incrementAndGet()
                                    if (afterEachFails)
                                        throw RuntimeException("aftereach error message")
                                }
                                test("first test") {
                                    if (testsFail) throw RuntimeException("test 1 error message")
                                }
                                test("second test") {
                                    if (testsFail) throw RuntimeException("test 2 error message")
                                }
                            }
                            .run(silent = true)

                    fun assertOk(result: SuiteResult) {
                        softly {
                            assert(autoCloseCalled.get() == 2)
                            assert(afterEachCalled.get() == 2)
                            assertFailedGracefully(result)
                        }
                    }

                    describe("calls autoCLose, afterEach and reports test Failure") {
                        test("when the test fails and autoclose and aftereach work") {
                            val result =
                                suiteResult(
                                    testsFail = true,
                                    afterEachFails = false,
                                    autoCloseFails = false)
                            assertOk(result)
                        }
                        test("when the test fails and aftereach fails") {
                            val result =
                                suiteResult(
                                    testsFail = true, afterEachFails = true, autoCloseFails = false)
                            assertOk(result)
                        }
                        test("when the test succeeds, autoclose fails and aftereach works") {
                            val result =
                                suiteResult(
                                    testsFail = false,
                                    afterEachFails = true,
                                    autoCloseFails = false)
                            assertOk(result)
                        }
                        test("when the test and autoclose succeeds and aftereach fails") {
                            val result =
                                suiteResult(
                                    testsFail = false,
                                    afterEachFails = false,
                                    autoCloseFails = true)
                            assertOk(result)
                        }
                    }

                    it("reports the test failure even when the close callback fails too") {
                        var autocloseCalled = false
                        val result =
                            Suite {
                                    autoClose(null) {
                                        autocloseCalled = true
                                        throw RuntimeException("error message")
                                    }
                                    test("first test") { throw RuntimeException() }
                                    test("second test") { throw RuntimeException() }
                                }
                                .run(silent = true)
                        assert(autocloseCalled)
                        assertFailedGracefully(result)
                    }
                }
            }
            describe("after suite callback") {
                it("is called exactly once at the end of the suite, after all tests are finished") {
                    val events = CopyOnWriteArrayList<String>()
                    val result =
                        Suite {
                                afterSuite { events.add("afterSuite callback") }
                                test("first  test") { events.add("first test") }
                                // this subcontext is here to make sure the context executor has
                                // to execute the dsl twice
                                // to test that this does not create duplicate after suite
                                // callbacks.
                                context("sub context") {
                                    afterSuite { events.add("afterSuite callback in subcontext") }

                                    test("sub context test") { events.add("sub context test") }
                                    context("another subcontext") { test("with a test") {} }
                                }
                                test("second test") { events.add("second test") }
                            }
                            .run(silent = true)
                    assert(result.allOk)
                    assert(
                        events.takeLast(2).toSet() ==
                            setOf("afterSuite callback", "afterSuite callback in subcontext"))
                    assert(
                        events.toSet() ==
                            setOf(
                                "first test",
                                "second test",
                                "sub context test",
                                "afterSuite callback",
                                "afterSuite callback in subcontext"))
                }
                it("can throw exceptions that are ignored") {
                    val events = CopyOnWriteArrayList<String>()
                    val result =
                        Suite {
                                afterSuite { events.add("afterSuite callback") }
                                afterSuite { throw AssertionError() }
                                afterSuite { throw RuntimeException() }
                            }
                            .run(silent = true)
                    assert(result.allOk)
                    assert(events == listOf("afterSuite callback"))
                }
            }
        }
}
