package failgood.internal

import failgood.Context
import failgood.ContextDSL
import failgood.ContextLambda
import failgood.ExecutionListener
import failgood.FailGoodException
import failgood.Failed
import failgood.NullExecutionListener
import failgood.Pending
import failgood.ResourcesDSL
import failgood.RootContext
import failgood.SourceInfo
import failgood.Success
import failgood.TestDescription
import failgood.TestLambda
import failgood.TestPlusResult
import failgood.TestResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout

internal class ContextExecutor @OptIn(DelicateCoroutinesApi::class) constructor(
    private val rootContext: RootContext,
    val scope: CoroutineScope = GlobalScope,
    lazy: Boolean = false,
    val listener: ExecutionListener = NullExecutionListener,
    val testFilter: TestFilter = ExecuteAllTests,
    val timeoutMillis: Long = 40000L
) {

    val coroutineStart: CoroutineStart = if (lazy) CoroutineStart.LAZY else CoroutineStart.DEFAULT
    private var startTime = System.nanoTime()

    private val foundContexts = mutableListOf<Context>()
    private val deferredTestResults = LinkedHashMap<TestDescription, Deferred<TestPlusResult>>()
    private val processedTests = LinkedHashSet<ContextPath>()
    private val afterSuiteCallbacks = mutableSetOf<suspend () -> Unit>()
    private val investigatedContexts = mutableSetOf<Context>()

    /**
     * Execute the rootContext.
     *
     * We keep executing the Context DSL until we know about all contexts and tests in this root context
     * The first test in each context is directly executed (async via coroutines), and for all other tests in that
     * context we create a SingleTestExecutor that executes the whole context path of that test together with the test.
     *
     */
    suspend fun execute(): ContextResult {
        if (!testFilter.shouldRun(rootContext))
            return ContextInfo(listOf(), mapOf(), setOf())
        val function = rootContext.function
        val rootContext = Context(rootContext.name, null, rootContext.sourceInfo, rootContext.isolation)
        try {
            withTimeout(timeoutMillis) {
                while (true) {
                    startTime = System.nanoTime()
                    val resourcesCloser = ResourcesCloser(scope)
                    val visitor = ContextVisitor(rootContext, resourcesCloser)
                    visitor.function()
                    investigatedContexts.add(rootContext)
                    if (!rootContext.isolation) {
                        afterSuiteCallbacks.add { resourcesCloser.close() }
                        break
                    }
                    if (!visitor.contextsLeft) break
                }
            }
        } catch (e: Exception) {
            return FailedContext(rootContext, e)
        }
        // context order: first root context, then sub-contexts ordered by line number
        val contexts = listOf(rootContext) + foundContexts.sortedBy { it.sourceInfo!!.lineNumber }
        return ContextInfo(contexts, deferredTestResults, afterSuiteCallbacks)
    }

    private inner class ContextVisitor(
        private val parentContext: Context,
        private val resourcesCloser: ResourcesCloser
    ) : ContextDSL, ResourcesDSL by resourcesCloser {
        val isolation = parentContext.isolation
        private val contextInvestigated = investigatedContexts.contains(parentContext)
        private val namesInThisContext = mutableSetOf<String>() // test and context names to detect duplicates

        // we only run the first new test that we find here. the remaining tests of the context
        // run with the TestExecutor.
        private var ranATest = false
        var contextsLeft = false // are there sub contexts left to run?

        override suspend fun test(name: String, function: TestLambda) {
            checkName(name)
            val testPath = ContextPath(parentContext, name)
            if (!testFilter.shouldRun(testPath))
                return
            // we process each test only once
            if (!processedTests.add(testPath)) {
                return
            }
            val testDescription = TestDescription(parentContext, name, sourceInfo())
            if (!ranATest || !isolation) {
                // if we don't need isolation we run all tests here.
                // if we do:
                // we did not yet run a test, so we are going to run this test ourselves
                ranATest = true

                deferredTestResults[testDescription] = scope.async(start = coroutineStart) {

                    listener.testStarted(testDescription)
                    val testResult = executeTest(testDescription, function)
                    val testPlusResult = TestPlusResult(testDescription, testResult)
                    listener.testFinished(testPlusResult)
                    testPlusResult
                }
            } else {
                val resourcesCloser = ResourcesCloser(scope)
                val deferred = scope.async(start = coroutineStart) {
                    withTimeout(timeoutMillis) {

                        listener.testStarted(testDescription)
                        val result =
                            SingleTestExecutor(
                                rootContext,
                                testPath,
                                TestContext(resourcesCloser, listener, testDescription),
                                resourcesCloser
                            ).execute()
                        val testPlusResult = TestPlusResult(testDescription, result)
                        listener.testFinished(testPlusResult)
                        testPlusResult
                    }
                }
                deferredTestResults[testDescription] = deferred
            }
        }

        private suspend fun executeTest(testDescription: TestDescription, function: TestLambda): TestResult {
            return withTimeout(timeoutMillis) {
                try {
                    TestContext(resourcesCloser, listener, testDescription).function()
                } catch (e: Throwable) {
                    if (isolation) try {
                        resourcesCloser.close()
                    } catch (_: RuntimeException) {
                    }
                    return@withTimeout Failed(e)
                }
                if (isolation) {
                    try {
                        resourcesCloser.close()
                    } catch (e: Exception) {
                        return@withTimeout Failed(e)
                    }
                }
                Success((System.nanoTime() - startTime) / 1000)
            }
        }

        override suspend fun context(name: String, function: ContextLambda) {
            checkName(name)
            // if we already ran a test in this context we don't need to visit the child context now
            if (isolation && ranATest) {
                contextsLeft = true
                // but we need to run the root context again to visit this child context
                return
            }
            val contextPath = ContextPath(parentContext, name)
            if (!testFilter.shouldRun(contextPath))
                return

            if (processedTests.contains(contextPath)) return
            val sourceInfo = sourceInfo()
            val context = Context(name, parentContext, sourceInfo, isolation = isolation)
            val visitor = ContextVisitor(context, resourcesCloser)
            try {
                visitor.function()
                investigatedContexts.add(context)
            } catch (exceptionInContext: Throwable) {
                val testDescriptor = TestDescription(parentContext, name, sourceInfo)

                processedTests.add(contextPath) // don't visit this context again
                val testPlusResult = TestPlusResult(testDescriptor, Failed(exceptionInContext))
                deferredTestResults[testDescriptor] = CompletableDeferred(testPlusResult)
                listener.testFinished(testPlusResult)
                ranATest = true
                return
            }
            if (visitor.contextsLeft) {
                contextsLeft = true
            } else {
                foundContexts.add(context)
                processedTests.add(contextPath)
            }
            sourceInfo().lineNumber

            if (visitor.ranATest) ranATest = true
        }

        private fun checkName(name: String) {
            if (!namesInThisContext.add(name))
                throw DuplicateNameInContextException("duplicate name \"$name\" in context \"${parentContext.name}\"")
        }

        override suspend fun describe(name: String, function: ContextLambda) {
            context(name, function)
        }

        override suspend fun it(behaviorDescription: String, function: TestLambda) {
            test(behaviorDescription, function)
        }

        override suspend fun pending(behaviorDescription: String, function: TestLambda) {
            val testPath = ContextPath(parentContext, behaviorDescription)

            if (processedTests.add(testPath)) {
                val testDescriptor =
                    TestDescription(parentContext, behaviorDescription, sourceInfo())
                val result = Pending

                @Suppress("DeferredResultUnused")
                val testPlusResult = TestPlusResult(testDescriptor, result)
                deferredTestResults[testDescriptor] = CompletableDeferred(testPlusResult)
                listener.testFinished(testPlusResult)
            }
        }

        override fun afterSuite(function: suspend () -> Unit) {
            if (!contextInvestigated)
                afterSuiteCallbacks.add(function)
        }
    }

    private class DuplicateNameInContextException(s: String) : FailGoodException(s)

    private fun sourceInfo() =
        SourceInfo(RuntimeException().stackTrace.first { !(it.fileName?.endsWith("ContextExecutor.kt") ?: true) }!!)
}
