package failgood

import failgood.mock.mock
import failgood.mock.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isA
import strikt.assertions.isEqualTo

@Test
class SuiteTest {
    val context =
        describe(Suite::class) {
            test("Empty Suite fails") { expectThrows<RuntimeException> { Suite(listOf<ContextProvider>()) } }
            test("Suite {} creates a root context") {
                expectThat(Suite { test("test") {} }.contextProviders.single().getContexts().single().name)
                    .isEqualTo("root")
            }
            test("runSingleTest works") {
                expectThat(
                    Suite {
                        test("test") {
                            println("")
                        }
                    }.runSingle("root > test")
                ).isA<Success>()
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
                pending("treats errors in getContexts as failed context") {
                    val scope = CoroutineScope(Dispatchers.Unconfined)
                    val objectContextProvider = mock<ContextProvider>()
                    whenever(objectContextProvider) { getContexts() }.then { throw RuntimeException() }

                    Suite(listOf(objectContextProvider)).findTests(scope)
                }
            }
        }
}
