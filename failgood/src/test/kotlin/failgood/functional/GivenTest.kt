@file:Suppress("unused", "UNUSED_PARAMETER")

package failgood.functional

import failgood.*
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/*
 * given support will be a given!
 */

@Test
class GivenTest {
    val context = describe("Injecting Test Dependencies") {
        test("the context can create test dependencies") {
            val context = RootContext("TestContext for dependency Injection") {
                context(
                    "context with dependency lambda",
                    given = { "StringDependency" }  /* optional teardown*/
                ) {
                    test("test that takes a string dependency") { string ->
                        expectThat(string).isEqualTo("StringDependency")
                    }
                }
            }
            assert(Suite(context).run(silent = true).allOk)
        }
    }


}

