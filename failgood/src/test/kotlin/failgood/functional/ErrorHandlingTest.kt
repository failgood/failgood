package failgood.functional

import failgood.*
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isNotNull
import strikt.assertions.single
import strikt.assertions.startsWith
import java.util.UUID

@Test
class ErrorHandlingTest {
    val context = describe("Error Handling") {
        describe("Non deterministic test names") {
            it("make their test count as failed") {
                expectThat(
                    Suite {
                        it("test1" + UUID.randomUUID().toString()) {}
                        it("test2" + UUID.randomUUID().toString()) {}
                    }.run(silent = true).failedTests
                ).single().and {
                    get { test.testName }.startsWith("test2")
                    get { result }.isA<Failed>().get { failure.message }.isNotNull()
                        .contains("please make sure your test names contain no random parts")
                }
            }
        }
        test("tests with wrong receiver") {
            assert(Suite {

                // in the next line the `ContextDSL.` receiver is missing, so it adds the test to the outer context,
                // not the context that it is called from. this is now detected by treating only the current context as mutable,
                // and throw when tests are added to other contexts
                suspend fun /*ContextDSL.*/testCreator() {
                    it("test1") {}
                    it("test2") {}
                }
                describe("context 1") {
                    testCreator()
                }
                describe("context 2") {
                    testCreator()
                }
            }.run().failedContexts.singleOrNull()?.context?.name  == "root")

        }
    }
}
