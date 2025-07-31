package failgood.functional

import failgood.Failure
import failgood.Suite
import failgood.Test
import failgood.TestCollection
import failgood.testCollection
import java.util.UUID
import kotlin.test.assertEquals

/*
 * given support is a given
 */

@Test
class GivenTest {
    val tests = testCollection {
        it("passes the value of the contests given block to the test") {
            val context =
                testCollection(
                    "TestContext for dependency Injection", given = { "root dependency" }) {
                        context("context with given function", given = { "StringDependency" }) {
                            test("test that takes a string dependency") {
                                assertEquals("StringDependency", given)
                            }
                            test("second test that takes a string dependency") {
                                assertEquals("StringDependency", given)
                            }
                        }
                        describe(
                            "describe context with given function",
                            given = { "StringDependency" }) {
                                test("test that takes a string dependency") {
                                    assertEquals("StringDependency", given)
                                }
                                test("second test that takes a string dependency") {
                                    assertEquals("StringDependency", given)
                                }
                            }
                        describe(
                            "describe context with dependency function",
                            given = { "StringDependency" }) {
                                it("test that takes a string dependency") {
                                    assertEquals("StringDependency", given)
                                }
                                it("second test that takes a string dependency") {
                                    assertEquals("StringDependency", given)
                                }
                            }
                    }
            assert(Suite(context).run(silent = true).allOk)
        }
        describe("error handling") {
            it("treats errors in the given block as test failures") {
                val context =
                    TestCollection("root") {
                        describe(
                            "context with a given that throws",
                            given = { throw RuntimeException("given error") }) {
                                it("will make the first tests fail") {}
                                it("will make the second tests fail") {}
                            }
                    }
                val result = Suite(context).run(silent = true).allTests
                assert(
                    result.size == 2 &&
                        result.all {
                            it.isFailure && (it.result as Failure).failure.message == "given error"
                        })
                assert(
                    result.map { it.test.testName } ==
                        listOf("will make the first tests fail", "will make the second tests fail"))
            }
        }
        describe("nested describe") {
            describe("the given of a parent context", given = { "parentContextGiven" }) {
                describe(
                    "is available in the given block of the child",
                    given = {
                        val parentGiven = given()
                        assertEquals("parentContextGiven", parentGiven)
                        "ok"
                    }) {
                        it("first test") { assertEquals("ok", given) }
                        it("second test") { assertEquals("ok", given) }
                    }
            }
        }
        describe("given that accesses non given values") {
            val uuid = UUID.randomUUID()
            describe(
                "a context with given that uses a value from the parent context",
                given = { "my uuid is $uuid" }) {
                    describe(
                        "a context that uses the parent context value",
                        given = { given() + " and then the child context mutated it" }) {
                            it("first test") {
                                assertEquals(
                                    "my uuid is $uuid and then the child context mutated it", given)
                            }
                            it("second test") {
                                assertEquals(
                                    "my uuid is $uuid and then the child context mutated it", given)
                            }
                        }
                }
        }
    }
}
