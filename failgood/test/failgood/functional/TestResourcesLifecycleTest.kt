package failgood.functional

import failgood.Failure
import failgood.Suite
import failgood.SuiteResult
import failgood.Test
import failgood.describe
import failgood.mock.call
import failgood.mock.getCalls
import failgood.mock.mock
import failgood.mock.verify
import java.util.concurrent.CopyOnWriteArrayList
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isFalse
import strikt.assertions.isSameInstanceAs
import strikt.assertions.isTrue
import strikt.assertions.map

@Test
class TestResourcesLifecycleTest {
    val context =
        describe("closing test resources") {
            describe("autoclosable") {
                it("is closed in reverse order of creation") {
                    val closeable1 = mock<AutoCloseable>()
                    val closeable2 = mock<AutoCloseable>()
                    var resource1: AutoCloseable? = null
                    var resource2: AutoCloseable? = null
                    val totalEvents = CopyOnWriteArrayList<List<String>>()
                    expectThat(
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
                        )
                        .get { allOk }
                        .isFalse()
                    expectThat(totalEvents)
                        .containsExactlyInAnyOrder(
                            listOf("first test", "second close callback", "first close callback"),
                            listOf("second test", "second close callback", "first close callback"),
                            listOf(
                                "first failing test",
                                "second close callback",
                                "first close callback"
                            ),
                            listOf(
                                "second failing test",
                                "second close callback",
                                "first close callback"
                            )
                        )
                    expectThat(resource1).isSameInstanceAs(closeable1)
                    expectThat(resource2).isSameInstanceAs(closeable2)
                    expectThat(getCalls(closeable1))
                        .containsExactly(List(4) { call(AutoCloseable::close) })
                    expectThat(getCalls(closeable2))
                        .containsExactly(List(4) { call(AutoCloseable::close) })
                }
                it("closes autocloseables without callback") {
                    var ac1: AutoCloseable? = null
                    var ac2: AutoCloseable? = null
                    var resource1: AutoCloseable? = null
                    var resource2: AutoCloseable? = null
                    val totalEvents = CopyOnWriteArrayList<List<String>>()
                    expectThat(
                            Suite {
                                    val events = mutableListOf<String>()
                                    totalEvents.add(events)
                                    ac1 = AutoCloseable { events.add("first close callback") }
                                    resource1 = autoClose(ac1!!)
                                    ac2 = AutoCloseable { events.add("second close callback") }
                                    resource2 = autoClose(ac2!!)
                                    test("first test") { events.add("first test") }
                                    test("second test") { events.add("second test") }
                                }
                                .run(silent = true)
                        )
                        .get { allOk }
                        .isTrue()
                    expectThat(totalEvents)
                        .containsExactly(
                            listOf("first test", "second close callback", "first close callback"),
                            listOf("second test", "second close callback", "first close callback")
                        )
                    expectThat(resource1).isSameInstanceAs(ac1)
                    expectThat(resource2).isSameInstanceAs(ac2)
                }
                it("works inside a test") {
                    val closeable1 = mock<AutoCloseable>()
                    val closeable2 = mock<AutoCloseable>()
                    var resource1: AutoCloseable? = null
                    var resource2: AutoCloseable? = null
                    val totalEvents = CopyOnWriteArrayList<List<String>>()
                    expectThat(
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
                        )
                        .get { allOk }
                        .isTrue()
                    expectThat(totalEvents)
                        .containsExactly(
                            listOf("first test", "first close callback"),
                            listOf("second test", "second close callback")
                        )
                    expectThat(resource1).isSameInstanceAs(closeable1)
                    expectThat(resource2).isSameInstanceAs(closeable2)
                    expectThat(getCalls(closeable1)).containsExactly(call(AutoCloseable::close))
                    expectThat(getCalls(closeable2)).containsExactly(call(AutoCloseable::close))
                    verify(closeable1) { close() }
                    verify(closeable2) { close() }
                }
                describe("error handling") {
                    fun assertFailedGracefully(result: SuiteResult) {
                        expectThat(result) {
                            get { allOk }.isFalse()
                            get { allTests }
                                .hasSize(2)
                                .all { get { this.result }.isA<Failure>() }
                                .map { it.test.testName }
                                .containsExactlyInAnyOrder("first test", "second test")
                        }
                    }

                    var afterEachCalled = 0
                    var autoCloseCalled = 0
                    fun suiteResult(
                        testsFail: Boolean,
                        afterEachFails: Boolean,
                        autoCloseFails: Boolean
                    ) =
                        Suite {
                                autoClose(null) {
                                    autoCloseCalled++
                                    if (autoCloseFails)
                                        throw RuntimeException("autoclose error message")
                                }
                                afterEach {
                                    afterEachCalled++
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

                    describe("when the test fails and autoclose and aftereach work") {
                        val result =
                            suiteResult(
                                testsFail = true,
                                afterEachFails = false,
                                autoCloseFails = false
                            )
                        it("calls autoclose callbacks") { assert(autoCloseCalled == 2) }
                        it("calls afterEach callbacks") { assert(afterEachCalled == 2) }
                        it("reports the test failure") { assertFailedGracefully(result) }
                    }
                    describe("when the test fails and aftereach fails") {
                        val result =
                            suiteResult(
                                testsFail = true,
                                afterEachFails = true,
                                autoCloseFails = false
                            )
                        it("calls autoclose callbacks") { assert(autoCloseCalled == 2) }
                        it("calls afterEach callbacks") { assert(afterEachCalled == 2) }
                        it("reports the test failure") { assertFailedGracefully(result) }
                    }
                    describe("when the test succeeds, autoclose fails and aftereach works") {
                        val result =
                            suiteResult(
                                testsFail = false,
                                afterEachFails = true,
                                autoCloseFails = false
                            )
                        it("calls autoclose callbacks") { assert(autoCloseCalled == 2) }
                        it("calls afterEach callbacks") { assert(afterEachCalled == 2) }
                        it("reports the test failure") { assertFailedGracefully(result) }
                    }
                    describe("when the test and autoclose succeeds and aftereach fails") {
                        val result =
                            suiteResult(
                                testsFail = false,
                                afterEachFails = false,
                                autoCloseFails = true
                            )
                        it("calls autoclose callbacks") { assert(autoCloseCalled == 2) }
                        it("calls afterEach callbacks") { assert(afterEachCalled == 2) }
                        it("reports the test failure") { assertFailedGracefully(result) }
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
                    expectThat(
                            Suite {
                                    afterSuite { events.add("afterSuite callback") }
                                    test("first  test") { events.add("first test") }
                                    // this subcontext is here to make sure the context executor has
                                    // to execute the dsl twice
                                    // to test that this does not create duplicate after suite
                                    // callbacks.
                                    context("sub context") {
                                        afterSuite {
                                            events.add("afterSuite callback in subcontext")
                                        }

                                        test("sub context test") { events.add("sub context test") }
                                        context("another subcontext") { test("with a test") {} }
                                    }
                                    test("second test") { events.add("second test") }
                                }
                                .run(silent = true)
                        )
                        .get { allOk }
                        .isTrue()
                    expectThat(events.takeLast(2))
                        .containsExactlyInAnyOrder(
                            "afterSuite callback",
                            "afterSuite callback in subcontext"
                        )
                    expectThat(events)
                        .containsExactlyInAnyOrder(
                            "first test",
                            "second test",
                            "sub context test",
                            "afterSuite callback",
                            "afterSuite callback in subcontext"
                        )
                }
                it("can throw exceptions that are ignored") {
                    val events = CopyOnWriteArrayList<String>()
                    expectThat(
                            Suite {
                                    afterSuite { events.add("afterSuite callback") }
                                    afterSuite { throw AssertionError() }
                                    afterSuite { throw RuntimeException() }
                                }
                                .run(silent = true)
                        )
                        .get { allOk }
                        .isTrue()
                    expectThat(events).containsExactly("afterSuite callback")
                }
            }
        }
}
