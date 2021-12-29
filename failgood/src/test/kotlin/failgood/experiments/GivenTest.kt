@file:Suppress("unused", "UNUSED_PARAMETER")

package failgood.experiments

import failgood.ContextDSL
import failgood.RootContext
import failgood.Test
import failgood.describe
import failgood.toSuite
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/*
 experimental stubs for a `given` api
 */

@Test
class TestDependenciesTest {
    val context = describe("Injecting Test Dependencies") {
        test("the context can create test dependencies") {
            val context = RootContext("TestContext for dependency Injection") {
                given(
                    "context with dependency lambda",
                    { "StringDependency" }, { /* optional teardown*/ }
                ) {
                    test2("test that takes a string dependency") { string ->
                        expectThat(string).isEqualTo("StringDependency")
                    }
                    describe(
                        "a child context that uses the parent dependencies." +
                            " for tests in this context both the parent and this context dependencies" +
                            " are constructed",
                        { parentDependency -> parentDependency + "AddedString" }
                    ) {
                        test2("another test that takes a string dependency") { string ->
                            expectThat(string).isEqualTo("StringDependencyAddedString")
                        }
                    }
                    given(
                        "a child context that does not use the parent dependency." +
                            " for tests in this context the parent context dependencies" +
                            " are not constructed",
                        { -> "TotallyNewString" }
                    ) {
                        test2("another test that takes a string dependency") { string ->
                            expectThat(string).isEqualTo("TotallyNewString")
                        }
                    }
                }
            }
            context.toSuite().run(silent = true)
        }
    }

    private fun <ContextDependency> ContextDSL.given(
        contextName: String,
        dependencies: suspend () -> ContextDependency,
        dependencyTeardown: suspend (ContextDependency) -> Unit = {},
        contextLambda: suspend ContextDSL2<ContextDependency>.() -> Unit
    ) {
    }

    private fun <ContextDependency, ParentContextDependency> ContextDSL2<ParentContextDependency>.describe(
        contextName: String,
        dependencies: suspend (ParentContextDependency) -> ContextDependency,
        dependencyTeardown: suspend (ContextDependency) -> Unit = {},
        contextLambda: suspend ContextDSL2<ContextDependency>.() -> Unit
    ) {
    }
}

interface ContextDSL2<T> : ContextDSL {
    suspend fun test2(name: String, function: suspend (T) -> Unit)
}
