package failgood.mock

import failgood.Test
import failgood.describe
import kotlin.test.assertNotNull

@Test
class MockExample {
    val context = describe("new mock syntax") {
        it("looks like this") {
            val userManager: UserManager = mock()
            // how to create a mock and define its return values:
            the(userManager) {
                method { stringReturningFunction() }.returns("resultString")
                method { functionWithParameters(anyInt(), anyString()) }.returns("resultString1")
                method { functionWithDataClassParameters(any()) }.returns("resultString2")
                method { functionThatReturnsNullableString() }.will { throw RuntimeException("oops I failed") }
            }
            // we call the methods and they indeed act like we expected
            assert(userManager.stringReturningFunction() == "resultString")
            assert(userManager.functionWithParameters(1, "blah") == "resultString1")
            assert(userManager.functionWithDataClassParameters(User("blah")) == "resultString2")
            val exception =
                assertNotNull(kotlin.runCatching { userManager.functionThatReturnsNullableString() }.exceptionOrNull())
            assert(exception is RuntimeException && exception.message == "oops I failed")

            // now here is how to assert with what parameters the mock was called

            // first the traditional verify way
            verify(userManager) { stringReturningFunction() }
            verify(userManager) { functionWithParameters(1, "blah") }
            verify(userManager) { functionWithDataClassParameters(User("blah")) }
            verify(userManager) { functionThatReturnsNullableString() }

            // then next line will throw because the parameters are different
            assertNotNull(kotlin.runCatching { verify(userManager) { functionWithParameters(2, "blah") } }
                .exceptionOrNull()).let {
                assert(
                    it is MockException && it.message ==
                            "expected call functionWithParameters(2, blah) never happened. calls: stringReturningFunction(), " +
                            "functionWithParameters(1, blah), " +
                            "functionWithDataClassParameters(User(name=blah)), " +
                            "functionThatReturnsNullableString()"
                )
            }
            // or the different way that gives you more flexibility: get the calls and use your assertion lib on them
            // this way you can assert on call order, or ignore certain parameter values. also you can just use a syntax you already know
            // this is the better way and probably the syntax that you should use.
            val calls = getCalls(userManager)
            assert(
                calls.containsAll(
                    listOf(
                        call(UserManager::stringReturningFunction),
                        call(UserManager::functionWithParameters, 1, "blah"),
                        call(UserManager::functionWithDataClassParameters, User("blah")),
                        call(UserManager::functionThatReturnsNullableString)
                    )
                )
            )
        }
    }
}
