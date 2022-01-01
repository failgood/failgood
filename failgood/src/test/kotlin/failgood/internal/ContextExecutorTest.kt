package failgood.internal

import failgood.Failed
import failgood.RootContext
import failgood.Success
import failgood.Test
import failgood.describe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.doesNotContain
import strikt.assertions.get
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull
import strikt.assertions.map
import strikt.assertions.message
import strikt.assertions.single

@Suppress("NAME_SHADOWING")
@Test
class ContextExecutorTest {
    private var assertionError: AssertionError? = null

    @OptIn(DelicateCoroutinesApi::class)
    val context = describe(ContextExecutor::class) {
        describe("with a valid root context") {
            val ctx = RootContext("root context") {
                test("test 1") {}
                test("test 2") {}
                test("failed test") {
                    assertionError = AssertionError("failed")
                    throw assertionError!!
                }
                context("context 1") {
                    context("context 2") { test("test 3") {} }
                }
                context("context 4") { test("test 4") {} }
            }
            describe("executing all the tests") {
                val contextResult = coroutineScope {
                    ContextExecutor(ctx, this).execute()
                }
                val contextInfo = expectThat(contextResult).isA<ContextInfo>().subject
                it("returns tests in the same order as they are declared in the file") {
                    expectThat(contextInfo).get { tests.keys }.map { it.testName }
                        .containsExactly("test 1", "test 2", "failed test", "test 3", "test 4")
                }
                it("returns deferred test results") {
                    val testResults = contextInfo.tests.values.awaitAll()
                    val successful = testResults.filter { it.isSuccess }
                    val failed = testResults - successful.toSet()
                    expectThat(successful.map { it.test.testName })
                        .containsExactly("test 1", "test 2", "test 3", "test 4")
                    expectThat(failed).map { it.test.testName }.containsExactly("failed test")
                }

                it("returns contexts in the same order as they appear in the file") {
                    expectThat(contextInfo.contexts).map { it.name }
                        .containsExactly("root context", "context 1", "context 2", "context 4")
                }
                it("reports time of successful tests") {
                    expectThat(
                        contextInfo.tests.values.awaitAll().map { it.result }
                            .filterIsInstance<Success>()
                    ).isNotEmpty()
                        .all { get { timeMicro }.isGreaterThanOrEqualTo(1) }
                }
                describe("reports failed tests") {
                    val failure =
                        contextInfo.tests.values.awaitAll().map { it.result }.filterIsInstance<Failed>().single()
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
                            ctx,
                            this,
                            testFilter = StringListTestFilter(listOf("root context", "test 1"))
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
                            ctx,
                            this,
                            testFilter = StringListTestFilter(listOf("other root context", "test 1"))
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
                val contextResult = coroutineScope {
                    ContextExecutor(ctx, this).execute()
                }
                expectThat(contextResult).isA<ContextInfo>()

                val contextInfo = contextResult as ContextInfo
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
                        ctx,
                        this,
                        lazy = true,
                        testFilter = ExecuteAllTests
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
                    test("test 1") {
                        delay(1000)
                    }
                }
                val scope = CoroutineScope(Dispatchers.Unconfined)
                withTimeout(100) {
                    expectThat(ContextExecutor(ctx, scope).execute()).isA<ContextInfo>()
                }
                scope.cancel()
            }
        }

        describe("failing sub contexts") {
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
            val contextInfo = coroutineScope {
                ContextExecutor(ctx, this).execute()
            }
            expectThat(contextInfo).isA<ContextInfo>()
            val results = contextInfo as ContextInfo

            it("reports a failing context as a failing test") {
                expectThat(results.tests.values.awaitAll().filter { it.isFailed }).single().and {
                    get { test }.and {
                        get { testName }.isEqualTo("context 1")
                        get { container.name }.isEqualTo("root context")
                        get { sourceInfo }.and {
                            get { lineNumber }.isEqualTo(getLineNumber(error) - 1)
                            get { className }.contains("ContextExecutorTest")
                        }
                    }
                }
            }
            it("does not report a failing context as a context") {
                expectThat(results.contexts).map { it.name }.doesNotContain("context 1")
            }
        }
        it("handles failing root contexts") {
            val ctx = RootContext("root context") {
                throw RuntimeException("root context failed")
            }
            val result = coroutineScope {
                ContextExecutor(ctx, this).execute()
            }
            expectThat(result).isA<FailedContext>()
            /*
            expectThat(results.tests.values).hasSize(1)
            val result = results.tests.values.single().await()
            expectThat(result) {
                get {isFailed}.isTrue()
            }*/
        }
        describe("detects duplicated tests") {
            it("fails with duplicate tests in one context") {
                val ctx = RootContext {
                    test("dup test name") {}
                    test("dup test name") {}
                }
                val result = coroutineScope {
                    ContextExecutor(ctx, this).execute()
                }
                expectThat(result).isA<FailedContext>().get { failure }.message.isNotNull()
                    .contains("duplicate name \"dup test name\" in context \"root\"")
            }
            it("does not fail when the tests with the same name are in different contexts") {
                val ctx = RootContext {
                    test("duplicate test name") {}
                    context("context") { test("duplicate test name") {} }
                }
                coroutineScope {
                    ContextExecutor(
                        ctx,
                        this,
                        testFilter = ExecuteAllTests
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
                val result = coroutineScope {
                    ContextExecutor(ctx, this).execute()
                }
                expectThat(result).isA<FailedContext>().get { failure }.message.isNotNull()
                    .contains("duplicate name \"dup ctx\" in context \"root\"")
            }
            it("does not fail when the contexts with the same name are in different contexts") {
                val ctx = RootContext {
                    test("same context name") {}
                    context("context") { test("same context name") {} }
                }
                coroutineScope {
                    ContextExecutor(
                        ctx,
                        this,
                        testFilter = ExecuteAllTests
                    ).execute()
                }
            }
            it("fails when a context has the same name as a test in the same contexts") {
                val ctx =
                    RootContext {
                        test("same name") {}
                        context("same name") {}
                    }
                val result = coroutineScope {
                    ContextExecutor(ctx, this).execute()
                }
                expectThat(result).isA<FailedContext>().get { failure }.message.isNotNull()
                    .contains("duplicate name \"same name\" in context \"root\"")
            }
        }
        describe("handles strange contexts correctly") {
            it("a context with only one pending test") {
                val context = RootContext {
                    describe("context") {
                        pending("pending") {
                        }
                    }
                    test("test") {}
                }
                val contextResult = coroutineScope {
                    ContextExecutor(context, this).execute()
                }
                expectThat(contextResult).isA<ContextResult>()
                (contextResult as ContextInfo).tests.values.awaitAll()
            }
            test("tests can not contain nested contexts") {
                // context("this does not even compile") {}
            }
        }
    }

    private fun getLineNumber(runtimeException: Throwable?): Int =
        runtimeException!!.stackTrace.first().lineNumber
}
