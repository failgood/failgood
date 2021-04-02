@file:Suppress("unused", "UNUSED_PARAMETER")

package failfast.experiments

import failfast.ContextDSL
import failfast.FailFast
import failfast.RootContext
import failfast.Suite
import failfast.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/*
 * outline how an alternative dsl could look like.
 * right now I think that I'm not going to implement it because its less intuitive, and i can have the same
 * effect by using normal context declared dependencies with `by dependency {....}`
 */
fun main() {
    FailFast.runTest()
}

object TestDependenciesTest {
    val context = describe("Injecting Test Dependencies") {
        test("the context can create test dependencies") {
            val context = RootContext("TestContext for dependency Injection") {
                describe("context with dependency lambda",
                    { "StringDependency" }, {/* optional teardown*/ }) {
                    test("test that takes a string dependency") { string ->
                        expectThat(string).isEqualTo("StringDependency")
                    }
                    describe(
                        "a child context that uses the parent dependencies. for tests in this context both the parent and this context dependencies are constructed",
                        { parentDependency -> parentDependency + "AddedString" }) {
                        test("another test that takes a string dependency") { string ->
                            expectThat(string).isEqualTo("StringDependencyAddedString")
                        }
                    }
                    describe(
                        "a child context that does not use the parent dependency. for tests in this context the parent context dependencies are not constructed",
                        { -> "TotallyNewString" }) {
                        test("another test that takes a string dependency") { string ->
                            expectThat(string).isEqualTo("TotallyNewString")
                        }
                    }
                }
            }
            Suite(context).run()
        }
    }

    private fun <ContextDependency> ContextDSL.describe(
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
    suspend fun test(name: String, function: suspend (T) -> Unit)
}
