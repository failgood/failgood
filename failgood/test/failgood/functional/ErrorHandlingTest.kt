package failgood.functional

import failgood.Failure
import failgood.Suite
import failgood.Test
import failgood.testCollection
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isNotNull
import strikt.assertions.single
import strikt.assertions.startsWith
import java.util.UUID
import kotlin.test.assertNotNull

@Test
class ErrorHandlingTest {
    val tests =
        testCollection("Error Handling") {
            describe("Non deterministic test names") {
                it("make their test count as failed") {
                    expectThat(
                        Suite {
                            it("test1" + UUID.randomUUID().toString()) {}
                            it("test2" + UUID.randomUUID().toString()) {}
                        }
                            .run(silent = true)
                            .failedTests
                    )
                        .single()
                        .and {
                            get { test.testName }.startsWith("test2")
                            get { result }
                                .isA<Failure>()
                                .get { failure.message }
                                .isNotNull()
                                .contains(
                                    "please make sure your test names contain no random parts"
                                )
                        }
                }
            }
            test("tests with wrong receiver") {
                val suiteResult =
                    Suite {
                        // in the next line the `ContextDSL.` receiver is missing, so it adds
                        // the test to the outer context,
                        // not the context that it is called from. this is now detected by
                        // treating only the current context as mutable,
                        // and throw when tests are added to other contexts
                        // correct:     suspend fun ContextDSL.testCreator() {
                        suspend fun testCreator() {
                            it("test1") {}
                            it("test2") {}
                        }
                        describe("context 1") { testCreator() }
                        describe("context 2") { testCreator() }
                    }
                        .run(silent = true)
                val failedContext = assertNotNull(suiteResult.failedRootContexts.singleOrNull())
                assert(failedContext.context.name == "root")
                assert(
                    failedContext.failure.message?.contains(
                        "Trying to create a test in the wrong context. Make sure functions that create tests have " +
                                "ContextDSL as receiver"
                    ) == true
                ) {
                    failedContext.failure.stackTraceToString()
                }
            }
            it("handles errors in resource correctly") {
                Suite {
                    dependency({ throw RuntimeException() })
                    it("test") {}
                }
                    .run(silent = true)
            }
        }
}
