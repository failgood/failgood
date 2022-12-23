package failgood.internal.execution.context

import failgood.*
import failgood.Ignored.*
import failgood.internal.*
import kotlinx.coroutines.*
import strikt.api.expectThat
import strikt.assertions.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertNotNull

@Suppress("NAME_SHADOWING")
@Test
class ContextExecutorTest {
    private var assertionError: AssertionError? = null

    @Suppress("SimplifiableCallChain")
    val context = describe(ContextExecutor::class) {
        describe("with a typical valid root context") {
            val ctx = RootContext("root context") {
                test("test 1") {
                    delay(1)
                }
                test("test 2") {
                    delay(1)
                }
                test("ignored test", ignored = Because("testing")) {}
                test("failed test") {
                    assertionError = AssertionError("failed")
                    throw assertionError!!
                }
                context("context 1") {
                    context("context 2") { test("test 3") {
                        delay(1)
                    } }
                }
                context("context 4") { test("test 4") {
                    delay(1)
                } }
            }
            describe("executing all the tests") {
                val contextInfo = assertNotNull(execute(ctx) as? ContextInfo)
                it("returns tests in the same order as they are declared in the file") {
                    assert(contextInfo.tests.keys.map { it.testName }
                        == listOf("test 1", "test 2", "ignored test", "failed test", "test 3", "test 4"))
                }
                it("returns deferred test results") {
                    val testResults = contextInfo.tests.values.awaitAll()
                    val successful = testResults.filter { it.isSuccess }
                    val failed = assertNotNull(testResults.filter { it.isFailure }.singleOrNull())
                    val skipped = assertNotNull(testResults.filter { it.isSkipped }.singleOrNull())
                    assert(successful.map { it.test.testName } == listOf(
                        "test 1",
                        "test 2",
                        "test 3",
                        "test 4"
                    ))
                    assert(failed.test.testName == "failed test")
                    assert(skipped.test.testName == "ignored test")
                }

                it("returns contexts in the same order as they appear in the file") {
                    expectThat(contextInfo.contexts).map { it.name }
                        .containsExactly("root context", "context 1", "context 2", "context 4")
                }
                it("reports time of successful tests") {
                    expectThat(
                        contextInfo.tests.values.awaitAll().map { it.result }
                            .filterIsInstance<Success>()
                    ).isNotEmpty().all { get { timeMicro }.isGreaterThanOrEqualTo(1) }
                }
                describe("reports failed tests") {
                    val failure =
                        contextInfo.tests.values.awaitAll().map { it.result }.filterIsInstance<Failure>().single()
                    it("reports exception for failed tests") {
                        expectThat(assertionError).isNotNull()
                        val assertionError = assertionError!!
                        expectThat(failure.failure) {
                            get { stackTraceToString() }.isEqualTo(assertionError.stackTraceToString())
                        }
                    }
                }
            }
            describe("executing a subset of tests") {
                it("can execute a subset of tests") {
                    val contextResult = coroutineScope {
                        ContextExecutor(
                            ctx, this, testFilter = StringListTestFilter(listOf("root context", "test 1"))
                        ).execute()
                    }
                    val contextInfo = expectThat(contextResult).isA<ContextInfo>().subject
                    expectThat(contextInfo) {
                        get { tests.keys }.map { it.testName }.containsExactly("test 1")
                        get { contexts }.map { it.name }.containsExactly("root context")
                    }
                }
                it("does not execute the context at all if the root name does not match") {
                    val contextResult = coroutineScope {
                        ContextExecutor(
                            ctx, this, testFilter = StringListTestFilter(listOf("other root context", "test 1"))
                        ).execute()
                    }
                    val contextInfo = expectThat(contextResult).isA<ContextInfo>().subject
                    expectThat(contextInfo) {
                        get { tests }.isEmpty()
                        get { contexts }.isEmpty()
                    }
                }
            }
            describe("reports line numbers") {
                var rootContextLine = 0
                var context1Line = 0
                var context2Line = 0
                var test1Line = 0
                var test2Line = 0
                val ctx = RootContext("root context") {
                    rootContextLine = RuntimeException().stackTrace.first().lineNumber - 1
                    describe("context 1") {
                        context1Line = RuntimeException().stackTrace.first().lineNumber - 1
                        it("test1") {
                            test1Line = RuntimeException().stackTrace.first().lineNumber - 1
                        }
                    }
                    describe("context 2") {
                        context2Line = RuntimeException().stackTrace.first().lineNumber - 1
                        it("test2") {
                            test2Line = RuntimeException().stackTrace.first().lineNumber - 1
                        }
                    }
                }
                val contextInfo = assertNotNull(execute(ctx) as? ContextInfo)
                it("returns file info for all subcontexts") {
                    expectThat(contextInfo.contexts).all {
                        get { sourceInfo }.isNotNull().and {
                            get { fileName }.isEqualTo("ContextExecutorTest.kt")
                        }
                    }
                }
                it("returns line number for contexts") {
                    expectThat(contextInfo.contexts) {
                        get(0).get { sourceInfo }.isNotNull().get { lineNumber }.isEqualTo(rootContextLine)
                        get(1).get { sourceInfo }.isNotNull().get { lineNumber }.isEqualTo(context1Line)
                        get(2).get { sourceInfo }.isNotNull().get { lineNumber }.isEqualTo(context2Line)
                    }
                }
                it("reports file name for all tests") {
                    expectThat(contextInfo.tests.keys).all {
                        get { sourceInfo }.and {
                            get { fileName }.isEqualTo("ContextExecutorTest.kt")
                        }
                    }
                }
                it("reports line number for all tests") {
                    expectThat(contextInfo.tests.keys.toList()) {
                        get(0).get { sourceInfo }.get { lineNumber }.isEqualTo(test1Line)
                        get(1).get { sourceInfo }.get { lineNumber }.isEqualTo(test2Line)
                    }
                }
            }
        }
        describe("ignored tests") {
            it("are reported") {
                val result = execute(RootContext("root context") {
                    it("contains a single ignored test", ignored = Because("testing ignoring")) {
                    }
                })
                val contextInfo = assertNotNull(result as? ContextInfo)
                val test = assertNotNull(contextInfo.tests.keys.singleOrNull())
                assert(test.testName == "contains a single ignored test")
            }
        }
        describe("supports lazy execution") {
            it("postpones test execution until the deferred is awaited when lazy is set to true") {
                var testExecuted = false
                val ctx = RootContext("root context") {
                    test("test 1") {
                        testExecuted = true
                    }
                }
                coroutineScope {
                    val contextInfo = ContextExecutor(
                        ctx, this, lazy = true, testFilter = ExecuteAllTests
                    ).execute()

                    expectThat(testExecuted).isEqualTo(false)
                    expectThat(contextInfo).isA<ContextInfo>()
                    val deferred = (contextInfo as ContextInfo).tests.values.single()
                    expectThat(deferred.await().result).isA<Success>()
                    expectThat(testExecuted).isEqualTo(true)
                }
            }
        }
        describe("timing") {
            it("reports context structure before tests finish") {
                val ctx = RootContext("root context") {
                    repeat(10) {
                        test("test $it") {
                            delay(2000)
                        }
                    }
                }
                val scope = CoroutineScope(Dispatchers.Unconfined)
                // this timeout is huge because of slow ci, that does not mean it takes 1 second in normal use
                withTimeout(1000) {
                    assert(ContextExecutor(ctx, scope).execute() is ContextInfo)
                }
                scope.cancel()
            }
        }

        describe("when an exception happens inside a subcontext") {
            var error: Throwable? = null

            val ctx = RootContext("root context") {
                test("test 1") {}
                test("test 2") {}
                context("context 1") {
                    error = NotImplementedError("")
                    throw error!!
                }
                context("context 4") { test("test 4") {} }
            }
            val results = assertNotNull(execute(ctx) as? ContextInfo)

            it("it is reported as a failing test inside that context") {
                val failures = results.tests.values.awaitAll().filter { it.isFailure }
                val singleFailure = assertNotNull(failures.singleOrNull())
                // candidate for a soft assert
                with(singleFailure.test) {
                    assert(testName == "error in context")
                    assert(container.name == "context 1")
                    with(sourceInfo) {
                        assert(lineNumber == getLineNumber(error) - 1)
                        assert(className.contains("ContextExecutorTest"))
                    }
                }
            }
            it("reports the context as a context") {
                assert(results.contexts.map { it.name }.contains("context 1"))
            }
        }
        describe("an ignored sub-context") {
            val ctx = RootContext("root context") {
                test("test 1") {}
                test("test 2") {}
                context("context 1", ignored = Because("We are testing that it is correctly reported")) {
                }
                context("context 4") { test("test 4") {} }
            }
            val results = assertNotNull(execute(ctx) as? ContextInfo)

            it("it is reported as a skipped test inside that context") {
                val skippedTest = results.tests.values.awaitAll().filter { it.isSkipped }
                val singleSkippedTest = assertNotNull(skippedTest.singleOrNull())
                // candidate for a soft assert
                with(singleSkippedTest.test) {
                    assert(testName == "context ignored because We are testing that it is correctly reported")
                    assert(container.name == "context 1")
                    assert(sourceInfo.className.contains("ContextExecutorTest"))
                }
                assert((singleSkippedTest.result as Skipped).reason == "We are testing that it is correctly reported")
            }
            it("reports the context as a context") {
                assert(results.contexts.map { it.name }.contains("context 1"))
            }
        }

        it("handles failing root contexts") {
            val ctx = RootContext("root context") {
                throw RuntimeException("root context failed")
            }
            assert(execute(ctx) is FailedRootContext)
        }
        describe("detects duplicated tests") {
            it("fails with duplicate tests in one context") {
                val ctx = RootContext {
                    test("dup test name") {}
                    test("dup test name") {}
                }
                val result = execute(ctx)
                assert(
                    result is FailedRootContext &&
                        result.failure.message!!.contains("duplicate name \"dup test name\" in context \"root\"")
                )
            }
            it("does not fail when the tests with the same name are in different contexts") {
                val ctx = RootContext {
                    test("duplicate test name") {}
                    context("context") { test("duplicate test name") {} }
                }
                coroutineScope {
                    ContextExecutor(
                        ctx, this, testFilter = ExecuteAllTests
                    ).execute()
                }
            }
        }
        describe("detects duplicate contexts") {
            it("fails with duplicate contexts in one context") {
                val ctx = RootContext {
                    context("dup ctx") {}
                    context("dup ctx") {}
                }
                val result = execute(ctx)
                expectThat(result).isA<FailedRootContext>().get { failure }.message.isNotNull()
                    .contains("duplicate name \"dup ctx\" in context \"root\"")
            }
            it("does not fail when the contexts with the same name are in different contexts") {
                val ctx = RootContext {
                    test("same context name") {}
                    context("context") { test("same context name") {} }
                }
                coroutineScope {
                    ContextExecutor(
                        ctx, this, testFilter = ExecuteAllTests
                    ).execute()
                }
            }
            it("fails when a context has the same name as a test in the same contexts") {
                val ctx = RootContext {
                    test("same name") {}
                    context("same name") {}
                }
                val result = execute(ctx)
                expectThat(result).isA<FailedRootContext>().get { failure }.message.isNotNull()
                    .contains("duplicate name \"same name\" in context \"root\"")
            }
        }
        describe("filtering by tag") {
            val events = ConcurrentHashMap.newKeySet<String>()

            // start with the easy version
            it("can filter contexts in the root context by tag") {
                val context = RootContext {
                    describe("context without the tag") {
                        events.add("context without tag")
                    }
                    describe("context with the tag", tags = setOf("single")) {
                        events.add("context with tag")
                        it("should be executed") {
                            events.add("test in context with tag")
                        }
                        describe("subcontext of the context with the tag") {
                            events.add("context in context with tag")
                            it("should also be executed") {
                                events.add("test in context in context with tag")
                            }
                        }
                    }
                    test("test that should also not be executed") {
                        events.add("test in root context without tag")
                    }
                }
                val contextResult = execute(context, "single")
                expectSuccess(contextResult)
                expectThat(events).containsExactlyInAnyOrder(
                    "context with tag",
                    "test in context with tag",
                    "context in context with tag",
                    "test in context in context with tag"
                )
            }
            it("can filter tests in the root context by tag") {
                val context = RootContext {
                    describe("context without the tag") {
                        events.add("context without tag")
                    }
                    test("test with the tag", tags = setOf("single")) {
                        events.add("test in root context with tag")
                    }
                    it("other test with the tag", tags = setOf("single")) {
                        events.add("other test with the tag")
                    }
                    test("test that should also not be executed") {
                        events.add("test in root context without tag")
                    }
                }
                val contextResult = execute(context, tag = "single")
                expectSuccess(contextResult)
                expectThat(events).containsExactlyInAnyOrder(
                    "test in root context with tag",
                    "other test with the tag"
                )
            }
            it(
                "can filter tests in a subcontext", ignored =
                Because("This can currently not work " +
                    "because we don't find the tests in the subcontext without executing the context")
            ) {
                val context = RootContext {
                    describe("context without the tag") {
                        events.add("context without tag")
                        it("test in context without the tag") {
                            events.add("test in context without the tag")
                        }
                    }
                    describe("context without the tag that contains the test with the tag") {
                        events.add("context without the tag that contains the test with the tag")
                        test("test with the tag", tags = setOf("single")) {
                            events.add("test with the tag")
                        }
                    }
                    test("test in root context without tag") {
                        events.add("test in root context without tag")
                    }
                }
                val contextResult = execute(context, "single")
                expectSuccess(contextResult)
                expectThat(events).containsExactlyInAnyOrder(
                    "context without the tag that contains the test with the tag",
                    "test with the tag"
                )
            }
        }
        describe("handles strange contexts correctly") {
            it("a context with only one ignored test") {
                val context = RootContext {
                    describe("context") {
                        it("pending", ignored = Because("testing a pending test")) {}
                    }
                    test("test") {}
                }
                val contextResult = execute(context)
                expectSuccess(contextResult)
            }
            test("tests can not contain nested contexts") {
                // context("this does not even compile") {}
            }
        }
    }

    private suspend fun expectSuccess(contextResult: ContextResult) {
        expectThat(contextResult).isA<ContextInfo>().subject.tests.values.awaitAll()
    }

    private suspend fun execute(context: RootContext, tag: String? = null): ContextResult {
        return coroutineScope {
            ContextExecutor(context, this, runOnlyTag = tag).execute()
        }
    }

    private fun getLineNumber(runtimeException: Throwable?): Int = runtimeException!!.stackTrace.first().lineNumber
}
