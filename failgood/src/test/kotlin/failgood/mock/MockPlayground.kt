package failgood.mock

import failgood.Test
import failgood.describe

@Test
class MockPlayground {
    val context = describe("new mock syntax") {
        it("looks like this") {
            val userManager: IImpl = mock()
            the(userManager) {
                method { stringReturningFunction() }.returns("resultString")
                method { functionWithParameters(anyInt(), anyString()) }.returns("resultString1")
                method { functionWithDataClassParameters(any()) }.returns("resultString2")
                method { functionThatReturnsNullableString() }.will { throw RuntimeException() }
            }
            assert(userManager.stringReturningFunction() == "resultString")
            assert(userManager.functionWithParameters(1, "blah") == "resultString1")
            assert(userManager.functionWithDataClassParameters(User("blah")) == "resultString2")
        }
    }
}

@Suppress("UNCHECKED_CAST", "unused")
private fun <T> MockConfigureDSL<*>.any(): T = null as T
@Suppress("unused")
private fun MockConfigureDSL<*>.anyString(): String = ""
@Suppress("unused")
private fun MockConfigureDSL<*>.anyInt(): Int = 42
