@file:Suppress("unused")

package failgood.functional

import failgood.*
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/*
 * given support is a given
 */

@Test
class GivenTest {
    val context = describe("Given support") {
        it("passes the value of the contests given block to the test") {
            val context = RootContext("TestContext for dependency Injection") {
                context(
                    "context with dependency lambda",
                    given = { "StringDependency" }
                ) {
                    test("test that takes a string dependency") { string ->
                        expectThat(string).isEqualTo("StringDependency")
                    }
                    test("second test that takes a string dependency") { string ->
                        expectThat(string).isEqualTo("StringDependency")
                    }
                }
                describe(
                    "describe context with dependency lambda",
                    given = { "StringDependency" }
                ) {
                    test("test that takes a string dependency") { string ->
                        expectThat(string).isEqualTo("StringDependency")
                    }
                    test("second test that takes a string dependency") { string ->
                        expectThat(string).isEqualTo("StringDependency")
                    }
                }
                describe(
                    "describe context with dependency lambda that uses it",
                    given = { "StringDependency" }
                ) {
                    it("test that takes a string dependency") { string ->
                        expectThat(string).isEqualTo("StringDependency")
                    }
                    it("second test that takes a string dependency") { string ->
                        expectThat(string).isEqualTo("StringDependency")
                    }
                }
            }
            assert(Suite(context).run(silent = true).allOk)
        }
        describe("error handling") {
            it("treats errors in the given block as test failures") {
                val context = RootContext("root") {
                    describe("context with a given that throws", given = { throw RuntimeException("given error") }) {
                        it("will make the first tests fail") {}
                        it("will make the second tests fail") {}
                    }
                }
                val result = Suite(context).run(silent = true).allTests
                assert(
                    result.size == 2 && result.all {
                        it.isFailure && (it.result as Failure).failure.message == "given error"
                    }
                )
                assert(
                    result.map { it.test.testName } ==
                        listOf("will make the first tests fail", "will make the second tests fail")
                )
            }
        }
    }
}
