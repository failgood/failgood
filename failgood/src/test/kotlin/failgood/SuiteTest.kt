package failgood

import failgood.internal.FailedContext
import failgood.mock.mock
import failgood.mock.whenever
import kotlinx.coroutines.*
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import kotlin.test.assertNotNull

@Test
class SuiteTest {
    val context =
        describe(Suite::class) {
            test("Empty Suite fails") { expectThrows<RuntimeException> { Suite(listOf<ContextProvider>()) } }
            test("Suite {} creates a root context") {
                expectThat(Suite { test("test") {} }.contextProviders.single().getContexts().single().name)
                    .isEqualTo("root")
            }
            describe("coroutine scope") {
                it("does not wait for tests before returning context info") {
                    val contexts = (1..10).map {
                        RootContext("root context") {
                            repeat(10) {
                                test("test $it") {
                                    delay(1000)
                                }
                            }
                        }
                    }
                    val scope = CoroutineScope(Dispatchers.Unconfined)
                    val deferredResult = withTimeout(100) {
                        Suite(contexts).findTests(scope)
                    }
                    withTimeout(100) {
                        deferredResult.awaitAll()
                    }
                    scope.cancel()
                }
            }
            describe("error handling") {
                it("treats errors in getContexts as failed context") {
                    class MyErrorTest

                    val scope = CoroutineScope(Dispatchers.Unconfined)
                    val objectContextProvider = mock<ContextProvider>()
                    whenever(objectContextProvider) { getContexts() }.then {
                        throw ErrorLoadingContextsFromClass(
                            "theerror",
                            MyErrorTest::class.java,
                            RuntimeException("exception error")
                        )
                    }

                    val contextResult = assertNotNull(
                        Suite(listOf(objectContextProvider)).findTests(scope)
                            .singleOrNull()?.await()
                    )
                    assert(
                        contextResult is FailedContext && (
                            contextResult.failure.message == "the error" &&
                                contextResult.context.name == MyErrorTest::class.java.name
                            )
                    )
                }
            }
        }
}
