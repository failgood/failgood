package failgood.internal

import failgood.*
import kotlinx.coroutines.*

internal class ContextExecutor constructor(
    private val rootContext: RootContext,
    val scope: CoroutineScope,
    lazy: Boolean = false,
    val listener: ExecutionListener = NullExecutionListener,
    val testFilter: TestFilter = ExecuteAllTests,
    val timeoutMillis: Long = 40000L,
    val onlyTag: String? = null
) {
    val filteringByTag = onlyTag != null
    val coroutineStart: CoroutineStart = if (lazy) CoroutineStart.LAZY else CoroutineStart.DEFAULT
    private var startTime = System.nanoTime()

    // did we find contexts without isolation in ths root context?
    // in that case we have to call the resources closer after suite.
    private var containsContextsWithoutIsolation = !rootContext.isolation

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
                do {
                    startTime = System.nanoTime()
                    val resourcesCloser = ResourcesCloser(scope)
                    val visitor = ContextVisitor(
                        rootContext,
                        resourcesCloser,
                        given = {}

                    )
                    visitor.function()
                    investigatedContexts.add(rootContext)
                    if (containsContextsWithoutIsolation) {
                        afterSuiteCallbacks.add { resourcesCloser.closeAutoCloseables() }
                    }
                } while (visitor.contextsLeft)
            }
        } catch (e: Throwable) {
            return FailedRootContext(rootContext, e)
        }
        // context order: first root context, then sub-contexts ordered by line number
        val contexts = listOf(rootContext) + foundContexts.sortedBy { it.sourceInfo!!.lineNumber }
        return ContextInfo(contexts, deferredTestResults, afterSuiteCallbacks)
    }

    private inner class ContextVisitor<GivenType>(
        private val context: Context,
        private val resourcesCloser: ResourcesCloser,
        // execute subcontexts and tests regardless of their tags, even when filtering
        private val executeAll: Boolean = false,
        val given: (suspend () -> GivenType)
    ) : ContextDSL<GivenType>, ResourcesDSL by resourcesCloser {
        val isolation = context.isolation
        private val contextInvestigated = investigatedContexts.contains(context)
        private val namesInThisContext = mutableSetOf<String>() // test and context names to detect duplicates

        // we only run the first new test that we find here. the remaining tests of the context
        // run with the TestExecutor.
        private var ranATest = false
        var contextsLeft = false // are there sub contexts left to run?
        var mutable = true // we allow changes only to the current context to catch errors in the context structure

        override suspend fun test(name: String, tags: Set<String>, function: TestLambda<GivenType>) {
            checkForDuplicateName(name)
            if (!executeAll && (filteringByTag && !tags.contains(onlyTag)))
                return
            val testPath = ContextPath(context, name)
            if (!testFilter.shouldRun(testPath))
                return
            // we process each test only once
            if (!processedTests.add(testPath)) {
                return
            }
            val testDescription = TestDescription(context, name, sourceInfo())
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
                    listener.testStarted(testDescription)
                    val testPlusResult = try {
                        withTimeout(timeoutMillis) {
                            val result =
                                SingleTestExecutor(
                                    rootContext,
                                    testPath,
                                    TestContext(resourcesCloser, listener, testDescription),
                                    resourcesCloser
                                ).execute()
                            TestPlusResult(testDescription, result)
                        }
                    } catch (e: TimeoutCancellationException) {
                        TestPlusResult(testDescription, Failure(e))
                    }
                    testPlusResult.also {
                        listener.testFinished(it)
                    }
                }
                deferredTestResults[testDescription] = deferred
            }
        }

        private suspend fun executeTest(
            testDescription: TestDescription,
            function: TestLambda<GivenType>
        ): TestResult {
            return try {
                withTimeout(timeoutMillis) {
                    val testContext = TestContext(resourcesCloser, listener, testDescription)
                    try {
                        testContext.function(given.invoke())
                    } catch (e: Throwable) {
                        val failure = Failure(e)
                        try {
                            resourcesCloser.callAfterEach(testContext, failure)
                        } catch (_: Exception) {
                        } catch (_: AssertionError) {
                        }
                        if (isolation) try {
                            resourcesCloser.closeAutoCloseables()
                        } catch (_: Exception) {
                        } catch (_: AssertionError) {
                        }
                        return@withTimeout failure
                    }
                    // test was successful
                    val success = Success((System.nanoTime() - startTime) / 1000)
                    try {
                        resourcesCloser.callAfterEach(testContext, success)
                    } catch (e: Throwable) {
                        if (isolation) {
                            try {
                                resourcesCloser.closeAutoCloseables()
                            } catch (e: Throwable) {
                                return@withTimeout Failure(e)
                            }
                        }
                        return@withTimeout Failure(e)
                    }
                    if (isolation) {
                        try {
                            resourcesCloser.closeAutoCloseables()
                        } catch (e: Throwable) {
                            return@withTimeout Failure(e)
                        }
                    }
                    success
                }
            } catch (e: TimeoutCancellationException) {
                Failure(e)
            }
        }

        override suspend fun <ContextDependency> context(
            name: String,
            tags: Set<String>,
            isolation: Isolation?,
            given: (suspend () -> ContextDependency),
            contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
        ) {
            checkForDuplicateName(name)
            if (!executeAll && (filteringByTag && !tags.contains(onlyTag)))
                return
            if (isolation == Isolation.OFF)
                containsContextsWithoutIsolation = true
            // if we already ran a test in this context we don't need to visit the child context now
            if (this.isolation && ranATest) {
                contextsLeft = true
                // but we need to run the root context again to visit this child context
                return
            }
            val contextPath = ContextPath(context, name)
            if (!testFilter.shouldRun(contextPath))
                return

            if (processedTests.contains(contextPath)) return
            val sourceInfo = sourceInfo()
            val subContextShouldHaveIsolation = isolation != Isolation.OFF && this.isolation
            val context = Context(name, context, sourceInfo, subContextShouldHaveIsolation)
            val visitor = ContextVisitor(context, resourcesCloser, filteringByTag, given)
            this.mutable = false
            try {
                visitor.mutable = true
                visitor.contextLambda()
                visitor.mutable = false
                this.mutable = true
                investigatedContexts.add(context)
            } catch (exceptionInContext: ImmutableContextException) {
                // this is fatal, and we treat the whole root context as failed, so we just rethrow
                throw exceptionInContext
            } catch (exceptionInContext: Throwable) {
                this.mutable = true
                val testDescriptor = TestDescription(context, "error in context", sourceInfo)

                processedTests.add(contextPath) // don't visit this context again
                val testPlusResult = TestPlusResult(testDescriptor, Failure(exceptionInContext))
                deferredTestResults[testDescriptor] = CompletableDeferred(testPlusResult)
                foundContexts.add(context)
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

            if (visitor.ranATest) ranATest = true
        }

        override suspend fun context(
            name: String,
            tags: Set<String>,
            isolation: Isolation?,
            function: ContextLambda
        ) {
            context(name, tags, isolation, {}, function)
        }

        override suspend fun <ContextDependency> describe(
            name: String,
            tags: Set<String>,
            isolation: Isolation?,
            given: suspend () -> ContextDependency,
            contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
        ) = context(name, tags, isolation, given, contextLambda)

        private fun checkForDuplicateName(name: String) {
            if (!namesInThisContext.add(name))
                throw DuplicateNameInContextException("duplicate name \"$name\" in context \"${context.name}\"")
            if (!mutable) {
                throw ImmutableContextException(
                    "Trying to create a test in the wrong context. " +
                        "Make sure functions that create tests have ContextDSL as receiver"
                )
            }
        }

        override suspend fun describe(
            name: String,
            tags: Set<String>,
            isolation: Isolation?,
            function: ContextLambda
        ) {
            context(name, tags, isolation, function)
        }

        override suspend fun it(name: String, tags: Set<String>, function: TestLambda<GivenType>) {
            test(name, tags, function)
        }

        override suspend fun ignore(name: String, function: TestLambda<GivenType>) {
            val testPath = ContextPath(context, name)

            if (processedTests.add(testPath)) {
                val testDescriptor =
                    TestDescription(context, name, sourceInfo())
                val result = Pending

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
    class ImmutableContextException(s: String) : FailGoodException(s)

    private fun sourceInfo(): SourceInfo {
        val runtimeException = RuntimeException()
        // find the first stack trace element that is not in this class or ContextDSL
        // (ContextDSL because of default parameters defined there)
        return SourceInfo(
            runtimeException.stackTrace.first {
                !(
                    it.fileName?.let { fileName ->
                        fileName.endsWith("ContextExecutor.kt") ||
                            fileName.endsWith("ContextDSL.kt")
                    } ?: true
                    )
            }!!
        )
    }
}
