package failgood.internal.execution

import failgood.Context
import failgood.Failure
import failgood.SourceInfo
import failgood.Success
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.dsl.TestFunction
import failgood.internal.ContextPath
import failgood.internal.ResourcesCloser
import failgood.internal.SingleTestExecutor
import failgood.internal.TestContext
import failgood.internal.given.GivenDSLHandler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout

internal class ContextStateCollector<RootGiven>(
    private val staticConfig: StaticContextExecutionConfig<RootGiven>,
    var containsContextsWithoutIsolation: Boolean,
) {
    val foundContexts = mutableListOf<Context>()
    val deferredTestResults = mutableMapOf<TestDescription, Deferred<TestPlusResult>>()
    val afterSuiteCallbacks = mutableSetOf<suspend () -> Unit>()
    val investigatedContexts = mutableSetOf<Context>()
    val finishedPaths = mutableSetOf<ContextPath>()

    suspend fun recordContextAsFailed(
        context: Context,
        sourceInfo: SourceInfo,
        contextPath: ContextPath,
        exceptionInContext: Throwable,
    ) {
        val testDescriptor = TestDescription(context, "error in context", sourceInfo)
        val testPlusResult = TestPlusResult(testDescriptor, Failure(exceptionInContext))
        deferredTestResults[testDescriptor] = CompletableDeferred(testPlusResult)
        staticConfig.listener.testDiscovered(testDescriptor)
        finishedPaths.add(contextPath)
        foundContexts.add(context)
        staticConfig.listener.testStarted(testDescriptor)
        staticConfig.listener.testFinished(testPlusResult)
    }

    fun <GivenType> executeTest(
        testDescription: TestDescription,
        function: TestFunction<GivenType>,
        resourcesCloser: ResourcesCloser,
        isolation: Boolean,
        givenDSLHandler: GivenDSLHandler<GivenType>,
        rootContextStartTime: Long,
    ) {
        deferredTestResults[testDescription] =
            staticConfig.scope.async(executionContext(), start = staticConfig.coroutineStart) {
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
                                            resourcesCloser, listener, testDescription, null)
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
                                if (isolation) {
                                    try {
                                        resourcesCloser.closeAutoCloseables()
                                    } catch (_: Throwable) {}
                                }
                                return@withTimeout failure
                            }
                            val success =
                                Success((platformNanoTime() - rootContextStartTime) / 1000)
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
        val resourcesCloser = createResourcesCloser(staticConfig.scope)
        val deferred =
            staticConfig.scope.async(executionContext(), start = staticConfig.coroutineStart) {
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
                                            null,
                                        ),
                                        resourcesCloser,
                                        staticConfig.rootContextFunction,
                                        staticConfig.givenFunction,
                                    )
                                    .execute()
                            TestPlusResult(testDescription, result)
                        }
                    } catch (e: Throwable) {
                        TestPlusResult(testDescription, Failure(e))
                    }
                testPlusResult.also { staticConfig.listener.testFinished(it) }
            }
        deferredTestResults[testDescription] = deferred
    }
}
