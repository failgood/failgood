package failgood.internal.execution

import failgood.*
import failgood.dsl.TestFunction
import failgood.internal.*
import failgood.internal.given.GivenDSLHandler
import kotlinx.coroutines.*

internal class ContextStateCollector<RootGiven>(
    private val staticConfig: StaticContextExecutionConfig<RootGiven>,
    // did we find contexts without isolation in this root context?
    // in that case we have to call the resources closer after suite.
    var containsContextsWithoutIsolation: Boolean
) {

    // here we build a list of all the sub-contexts in this root context to later return it
    val foundContexts = mutableListOf<Context>()

    val deferredTestResults = mutableMapOf<TestDescription, Deferred<TestPlusResult>>()
    val afterSuiteCallbacks = mutableSetOf<suspend () -> Unit>()

    // a context is investigated when we have executed it once. we still need to execute it again to
    // get into its sub-contexts
    val investigatedContexts = mutableSetOf<Context>()

    // tests or contexts that we don't have to execute again.
    val finishedPaths = mutableSetOf<ContextPath>()

    /*
     * A context is reported as failure by reporting it as a context with a failed test as only child.
     */
    suspend fun recordContextAsFailed(
        context: Context,
        sourceInfo: SourceInfo,
        contextPath: ContextPath,
        exceptionInContext: Throwable
    ) {
        val testDescriptor = TestDescription(context, "error in context", sourceInfo)
        val testPlusResult = TestPlusResult(testDescriptor, Failure(exceptionInContext))
        deferredTestResults[testDescriptor] = CompletableDeferred(testPlusResult)
        staticConfig.listener.testDiscovered(testDescriptor)

        finishedPaths.add(contextPath) // don't visit this context again
        foundContexts.add(context)
        staticConfig.listener.testFinished(testPlusResult)
    }

    fun <GivenType> executeTest(
        testDescription: TestDescription,
        function: TestFunction<GivenType>,
        resourcesCloser: ResourcesCloser,
        isolation: Boolean,
        givenDSLHandler: GivenDSLHandler<GivenType>,
        rootContextStartTime: Long
    ) {
        deferredTestResults[testDescription] =
            staticConfig.scope.async(start = staticConfig.coroutineStart) {
                val listener = staticConfig.listener
                listener.testStarted(testDescription)
                val testResult =
                    try {
                        withTimeout(staticConfig.timeoutMillis) {
                            val given =
                                try {
                                    givenDSLHandler.given()
                                } catch (e: Throwable) {
                                    val failure = Failure(e)
                                    val testContext =
                                        TestContext(
                                            resourcesCloser,
                                            listener,
                                            testDescription,
                                            null
                                        )
                                    resourcesCloser.callAfterEach(testContext, failure)
                                    return@withTimeout failure
                                }
                            val testContext =
                                TestContext(resourcesCloser, listener, testDescription, given)
                            try {
                                testContext.function()
                            } catch (e: Throwable) {
                                val failure = Failure(e)
                                try {
                                    resourcesCloser.callAfterEach(testContext, failure)
                                } catch (_: Throwable) {}
                                if (isolation)
                                    try {
                                        resourcesCloser.closeAutoCloseables()
                                    } catch (_: Throwable) {}
                                return@withTimeout failure
                            }
                            // test was successful
                            val success = Success((System.nanoTime() - rootContextStartTime) / 1000)
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

    fun executeTestLater(testDescription: TestDescription, testPath: ContextPath) {
        val resourcesCloser = ResourceCloserImpl(staticConfig.scope)
        val deferred =
            staticConfig.scope.async(start = staticConfig.coroutineStart) {
                staticConfig.listener.testStarted(testDescription)
                val testPlusResult =
                    try {
                        withTimeout(staticConfig.timeoutMillis) {
                            val result =
                                SingleTestExecutor(
                                        testPath,
                                        TestContext(
                                            resourcesCloser,
                                            staticConfig.listener,
                                            testDescription,
                                            null
                                        ),
                                        resourcesCloser,
                                        staticConfig.rootContextFunction,
                                        staticConfig.givenFunction
                                    )
                                    .execute()
                            TestPlusResult(testDescription, result)
                        }
                    } catch (e: Throwable) {
                        TestPlusResult(testDescription, Failure(e))
                    } catch (e: TimeoutCancellationException) {
                        TestPlusResult(testDescription, Failure(e))
                    }
                testPlusResult.also { staticConfig.listener.testFinished(it) }
            }
        deferredTestResults[testDescription] = deferred
    }
}
