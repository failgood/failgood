@file:Suppress("unused", "UNUSED_PARAMETER", "UnusedReceiverParameter", "EmptyMethod")

package failgood.experiments.given

import failgood.*
import failgood.RootContext
import failgood.dsl.ContextDSL
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/*
the original given spike, with too many features for now.
*/

@Suppress("EmptyMethod")
@Test
class ComplexGivenTest {
    val context =
        describe("Injecting Test Dependencies") {
            test("the context can create test dependencies") {
                val context =
                    RootContext("TestContext for dependency Injection") {
                        given2(
                            "context with dependency lambda",
                            { "StringDependency" } // optional teardown
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
                            describe(
                                "a child context that does not use the parent dependency." +
                                    " for tests in this context the parent context dependencies" +
                                    " are not constructed",
                                given = { "TotallyNewString" }
                            ) {
                                this@given2.test2("another test that takes a string dependency") {
                                    string ->
                                    expectThat(string).isEqualTo("TotallyNewString")
                                }
                            }
                        }
                    }
                assert(Suite(context).run(silent = true).allOk)
            }
        }

    private fun <ContextDependency> ContextDSL<Unit>.given2(
        contextName: String,
        dependency: suspend () -> ContextDependency,
        dependencyTeardown: suspend (ContextDependency) -> Unit = {},
        contextLambda: suspend GivenDSL<ContextDependency>.() -> Unit
    ) {}

    private fun <ContextDependency, ParentContextDependency> GivenDSL<ParentContextDependency>
        .describe(
        contextName: String,
        dependencies: suspend (ParentContextDependency) -> ContextDependency,
        dependencyTeardown: suspend (ContextDependency) -> Unit = {},
        contextLambda: suspend GivenDSL<ContextDependency>.() -> Unit
    ) {}
}

interface GivenDSL<T> : ContextDSL<Unit> {
    suspend fun test2(name: String, function: suspend (T) -> Unit)
}
