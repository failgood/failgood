package failgood.internal.execution.context

import failgood.*
import failgood.internal.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout

internal class ContextVisitor<GivenType>(
    private val contextStateCollector: ContextStateCollector,
    private val context: Context,
    private val resourcesCloser: ResourcesCloser,
    // execute sub-contexts and tests regardless of their tags, even when filtering
    private val executeAll: Boolean = false,
    val given: suspend () -> GivenType,
    val onlyRunSubcontexts: Boolean // no need to run tests, just go into sub contexts
) : ContextDSL<GivenType>, ResourcesDSL by resourcesCloser {
    private val isolation = context.isolation
    private val namesInThisContext = mutableSetOf<String>() // test and context names to detect duplicates

    // we only run the first new test that we find here. the remaining tests of the context
    // run with the SingleTestExecutor.
    private var ranATest = false
    var contextsLeft = false // are there sub contexts left to run?
    private var mutable = true // we allow changes only to the current context to catch errors in the context structure

    override suspend fun it(name: String, tags: Set<String>, function: TestLambda<GivenType>) {
        if (onlyRunSubcontexts)
            return
        checkForDuplicateName(name)
        if (!executeAll && (contextStateCollector.runOnlyTag != null && !tags.contains(contextStateCollector.runOnlyTag)))
            return
        val testPath = ContextPath(context, name)
        if (!contextStateCollector.testFilter.shouldRun(testPath))
            return
        // we process each test only once
        if (!contextStateCollector.finishedPaths.add(testPath)) {
            return
        }
        val testDescription = TestDescription(context, name, sourceInfo())
        if (!ranATest || !isolation) {
            // if we don't need isolation we run all tests here.
            // if we do:
            // we did not yet run a test, so we are going to run this test ourselves
            ranATest = true

            executeTest(testDescription, function, resourcesCloser, isolation, given)
        } else {
            val resourcesCloser = OnlyResourcesCloser(contextStateCollector.scope)
            val deferred = contextStateCollector.scope.async(start = contextStateCollector.coroutineStart) {
                contextStateCollector.listener.testStarted(testDescription)
                val testPlusResult = try {
                    withTimeout(contextStateCollector.timeoutMillis) {
                        val result =
                            SingleTestExecutor(
                                contextStateCollector.rootContext,
                                testPath,
                                TestContext(resourcesCloser, contextStateCollector.listener, testDescription),
                                resourcesCloser
                            ).execute()
                        TestPlusResult(testDescription, result)
                    }
                } catch (e: TimeoutCancellationException) {
                    TestPlusResult(testDescription, Failure(e))
                }
                testPlusResult.also {
                    contextStateCollector.listener.testFinished(it)
                }
            }
            contextStateCollector.deferredTestResults[testDescription] = deferred
        }
    }

    private fun executeTest(
        testDescription: TestDescription,
        function: TestLambda<GivenType>,
        resourcesCloser: ResourcesCloser,
        isolation: Boolean,
        given: suspend () -> GivenType
    ) {
        contextStateCollector.deferredTestResults[testDescription] =
            contextStateCollector.scope.async(start = contextStateCollector.coroutineStart) {

                val listener = contextStateCollector.listener
                listener.testStarted(testDescription)
                val testResult = try {
                    withTimeout(contextStateCollector.timeoutMillis) {
                        val testContext = TestContext(resourcesCloser, listener, testDescription)
                        try {
                            testContext.function(given.invoke())
                        } catch (e: Throwable) {
                            val failure = Failure(e)
                            try {
                                resourcesCloser.callAfterEach(testContext, failure)
                            } catch (_: Throwable) {
                            }
                            if (isolation) try {
                                resourcesCloser.closeAutoCloseables()
                            } catch (_: Throwable) {
                            }
                            return@withTimeout failure
                        }
                        // test was successful
                        val success = Success((System.nanoTime() - contextStateCollector.startTime) / 1000)
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
                val testPlusResult = TestPlusResult(testDescription, testResult)
                listener.testFinished(testPlusResult)
                testPlusResult
            }
    }

    override suspend fun <ContextDependency> describe(
        name: String,
        tags: Set<String>,
        isolation: Boolean?,
        given: (suspend () -> ContextDependency),
        contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
    ) {
        checkForDuplicateName(name)
        if (!executeAll && (contextStateCollector.runOnlyTag != null && !tags.contains(contextStateCollector.runOnlyTag)))
            return
        if (isolation == false)
            contextStateCollector.containsContextsWithoutIsolation = true

        // if we already ran a test in this context we don't need to visit the child context now
        if (this.isolation && ranATest) {
            contextsLeft = true
            // but we need to run the root context again to visit this child context
            return
        }
        val contextPath = ContextPath(context, name)
        if (!contextStateCollector.testFilter.shouldRun(contextPath))
            return

        if (contextStateCollector.finishedPaths.contains(contextPath)) return
        val sourceInfo = sourceInfo()
        val subContextShouldHaveIsolation = isolation != false && this.isolation
        val context = Context(name, context, sourceInfo, subContextShouldHaveIsolation)
        if (isolation == true && !this.isolation) {
            contextStateCollector.recordContextAsFailed(
                context, sourceInfo, contextPath,
                FailGoodException("in a context without isolation it can not be turned on again")
            )
            return
        }
        val visitor =
            ContextVisitor(
                contextStateCollector,
                context,
                resourcesCloser,
                contextStateCollector.runOnlyTag != null,
                given,
                contextStateCollector.investigatedContexts.contains(context)
            )
        this.mutable = false
        try {
            visitor.mutable = true
            visitor.contextLambda()
            visitor.mutable = false
            this.mutable = true
            contextStateCollector.investigatedContexts.add(context)
        } catch (exceptionInContext: ImmutableContextException) {
            // this is fatal, and we treat the whole root context as failed, so we just rethrow
            throw exceptionInContext
        } catch (exceptionInContext: Throwable) {
            this.mutable = true
            contextStateCollector.recordContextAsFailed(context, sourceInfo, contextPath, exceptionInContext)
            ranATest = true
            return
        }
        if (visitor.contextsLeft) {
            contextsLeft = true
        } else {
            contextStateCollector.foundContexts.add(context)
            contextStateCollector.finishedPaths.add(contextPath)
        }

        if (visitor.ranATest) ranATest = true
    }

    override suspend fun describe(
        name: String,
        tags: Set<String>,
        isolation: Boolean?,
        function: ContextLambda
    ) {
        describe(name, tags, isolation, {}, function)
    }

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

    override suspend fun ignore(name: String, function: TestLambda<GivenType>) {
        if (onlyRunSubcontexts)
            return
        val testPath = ContextPath(context, name)

        if (contextStateCollector.finishedPaths.add(testPath)) {
            val testDescriptor =
                TestDescription(context, name, sourceInfo())
            val result = Pending

            val testPlusResult = TestPlusResult(testDescriptor, result)
            contextStateCollector.deferredTestResults[testDescriptor] = CompletableDeferred(testPlusResult)
            contextStateCollector.listener.testFinished(testPlusResult)
        }
    }

    override fun afterSuite(function: suspend () -> Unit) {
        if (!onlyRunSubcontexts)
            contextStateCollector.afterSuiteCallbacks.add(function)
    }
}
