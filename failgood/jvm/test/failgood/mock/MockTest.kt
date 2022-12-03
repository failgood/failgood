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
import kotlin.test.assertNotNull

@Test
class MockTest {
    val context = describe("the mocking framework") {
        val mock = mock<UserManager>()
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
        }
        describe("defining results") {
            describe("with the") {
                it("defines results via calling the mock") {
                    the(mock) {
                        method { stringReturningFunction() }.will { "resultString" }
                    }

                    expectThat(mock.stringReturningFunction()).isEqualTo("resultString")
                }
                it("mocks can throw") {
                    the(mock) { method { stringReturningFunction() }.will { throw RuntimeException("message") } }
                    expectThat(kotlin.runCatching { mock.stringReturningFunction() }
                        .exceptionOrNull()).isA<RuntimeException>().message.isEqualTo("message")
                }
                it("defines results via calling the mock even works for nullable functions") {
                    the(mock) { method { functionThatReturnsNullableString() }.will { "resultString" } }
                    expectThat(mock.functionThatReturnsNullableString()).isEqualTo("resultString")
                }
            }
            it("can be done when the mock is created") {
                val otherMock = mock<UserManager> {
                    method { stringReturningFunction() }.will { "resultString" }
                    method { functionThatReturnsNullableString() }.will { "otherResultString" }
                }
                expectThat(otherMock.stringReturningFunction()).isEqualTo("resultString")
                expectThat(otherMock.functionThatReturnsNullableString()).isEqualTo("otherResultString")
            }
            it("can call lambdas") {
                @Suppress("UNCHECKED_CAST")
                val m = mock<UserManager> {
                    method { functionThatHasLambdaParameter {} }.will {
                        (it.arguments.last() as () -> Unit)()
                    }
                }
                var lambdaCalled = false
                m.functionThatHasLambdaParameter { lambdaCalled = true }
                assert(lambdaCalled)
            }
        }
        it("can return function calls for normal asserting") {
            mock.function()
            mock.overloadedFunction()
            mock.overloadedFunction("string")
            mock.overloadedFunction(10)
            expectThat(getCalls(mock)).containsExactly(
                call(UserManager::function),
                call(UserManager::overloadedFunction),
                call(UserManager::overloadedFunction, "string"),
                call(UserManager::overloadedFunction, 10)
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
            expectThat(mock.toString()).isEqualTo("mock<UserManager>")
        }
        describe("error handling") {
            it("detects when the parameter to the is not a mock") {
                val exception = assertNotNull(
                    kotlin.runCatching {
                        the("not a mock") {}
                    }.exceptionOrNull()
                )
                assert(exception is MockException) { exception.stackTraceToString() }
            }
        }
        describe("parameter placeholders") {

            test("exist for all basic kotlin types") {
                mock<I> {
                    method {
                        methodWithAllTypes(
                            anyByte(),
                            anyShort(),
                            anyInt(),
                            anyLong(),
                            anyFloat(),
                            anyDouble(),
                            anyBoolean(),
                            anyChar(),
                            any(),
                            any()
                        )
                    }
                }
            }
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

private interface I {
    // see https://kotlinlang.org/docs/basic-types.html
    fun methodWithAllTypes(
        a: Byte,
        b: Short,
        c: Int,
        d: Long,
        e: Float,
        f: Double,
        g: Boolean,
        h: Char,
        i: String,
        j: Array<Byte>
    )
}
