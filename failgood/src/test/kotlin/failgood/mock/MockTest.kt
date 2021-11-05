package failgood.mock

import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.message

@Test
class MockTest {
    interface IImpl {
        fun overloadedFunction()
        fun overloadedFunction(s: String)
        fun overloadedFunction(i: Int)

        fun function()
        fun function2()
        fun functionWithParameters(number: Int, name: String)
        suspend fun suspendFunction(number: Int, name: String): String
        fun stringReturningFunction(): String
        fun functionThatReturnsNullableString(): String?
    }

    val context = describe("the mocking framework") {
        val mock = mock<IImpl>()
        describe("records function calls") {
            mock.function()
            it("verifies function calls") {
                verify(mock) { function() }
            }
            it("throws when a verified function was not called") {
                expectThrows<MockException> {
                    verify(mock) { function2() }
                }
            }
        }
        describe("records function parameters") {
            mock.functionWithParameters(10, "string")
            it("verifies function parameters") {
                verify(mock) { functionWithParameters(10, "string") }
            }
        }
        describe("supports suspend functions") {

            it("verifies suspend functions") {
                mock.suspendFunction(10, "string")
                verify(mock) { suspendFunction(10, "string") }
            }
            it("throws when parameters don't match") {
                mock.suspendFunction(10, "string")
                expectThrows<MockException> {
                    verify(mock) { suspendFunction(11, "string") }
                }
            }
            it("records results for suspend functions") {
                whenever(mock) { suspendFunction(0, "ignored") }.then { "suspendResultString" }
                expectThat(mock.suspendFunction(10, "string")).isEqualTo("suspendResultString")
            }
        }
        it("defines results via calling the mock") {
            whenever(mock) { stringReturningFunction() }.then { "resultString" }
            expectThat(mock.stringReturningFunction()).isEqualTo("resultString")
        }
        it("mocks can throw") {
            whenever(mock) { stringReturningFunction() }.then { throw RuntimeException("message") }
            expectThat(
                kotlin.runCatching { mock.stringReturningFunction() }
                    .exceptionOrNull()
            ).isA<RuntimeException>().message.isEqualTo("message")
        }
        it("defines results via calling the mock even works for nullable functions") {
            whenever(mock) { functionThatReturnsNullableString() }.then { "resultString" }
            expectThat(mock.functionThatReturnsNullableString()).isEqualTo("resultString")
        }
        it("can return function calls for normal asserting") {
            mock.function()
            mock.overloadedFunction()
            mock.overloadedFunction("string")
            mock.overloadedFunction(10)
            expectThat(getCalls(mock)).containsExactly(
                call(IImpl::function),
                call(IImpl::overloadedFunction),
                call(IImpl::overloadedFunction, "string"),
                call(IImpl::overloadedFunction, 10)
            )
        }
        it("has call helpers for up to 5 parameters") {
            call(InterfaceWithOverloadedMethods::function)
            call(InterfaceWithOverloadedMethods::function, "a")
            call(InterfaceWithOverloadedMethods::function, "a", "b")
            call(InterfaceWithOverloadedMethods::function, "a", "b", "c")
            call(InterfaceWithOverloadedMethods::function, "a", "b", "c", "d")
            call(InterfaceWithOverloadedMethods::function, "a", "b", "c", "d", "e")
        }
        it("has suspend call helpers for up to 5 parameters") {
            call(InterfaceWithOverloadedSuspendMethods::function)
            call(InterfaceWithOverloadedSuspendMethods::function, "a")
            call(InterfaceWithOverloadedSuspendMethods::function, "a", "b")
            call(InterfaceWithOverloadedSuspendMethods::function, "a", "b", "c")
            call(InterfaceWithOverloadedSuspendMethods::function, "a", "b", "c", "d")
            call(InterfaceWithOverloadedSuspendMethods::function, "a", "b", "c", "d", "e")
        }
        describe("handles equals correctly") {
            it("returns true for equals with the same mock") {
                expectThat(mock).isEqualTo(mock)
            }
            it("returns false for equals with a different object") {
                expectThat(mock).isNotEqualTo(mock())
            }
        }
        it("returns something useful as response to toString") {
            expectThat(mock.toString()).isEqualTo("mock<IImpl>")
        }
    }

    interface InterfaceWithOverloadedMethods {
        fun function()
        fun function(a: String)
        fun function(a: String, b: String)
        fun function(a: String, b: String, c: String)
        fun function(a: String, b: String, c: String, d: String)
        fun function(a: String, b: String, c: String, d: String, e: String)
    }

    interface InterfaceWithOverloadedSuspendMethods {
        suspend fun function()
        suspend fun function(a: String)
        suspend fun function(a: String, b: String)
        suspend fun function(a: String, b: String, c: String)
        suspend fun function(a: String, b: String, c: String, d: String)
        suspend fun function(a: String, b: String, c: String, d: String, e: String)
    }
}
