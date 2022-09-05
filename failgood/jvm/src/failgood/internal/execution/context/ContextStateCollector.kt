package failgood.internal.execution.context

import failgood.*
import failgood.internal.ContextPath
import failgood.internal.ResourcesCloser
import failgood.internal.TestContext
import kotlinx.coroutines.*

internal class ContextStateCollector(
    val staticConfig: StaticContextExecutionConfig,
    // did we find contexts without isolation in this root context?
    // in that case we have to call the resources closer after suite.
    var containsContextsWithoutIsolation: Boolean
) {

    // here we build a list of all the sub-contexts in this root context to later return it
    val foundContexts = mutableListOf<Context>()

    val deferredTestResults = LinkedHashMap<TestDescription, Deferred<TestPlusResult>>()
    val afterSuiteCallbacks = mutableSetOf<suspend () -> Unit>()

    // a context is investigated when we have executed it once. we still need to execute it again to get into its sub-contexts
    val investigatedContexts = mutableSetOf<Context>()

    // tests or contexts that we don't have to execute again.
    val finishedPaths = LinkedHashSet<ContextPath>()

    suspend fun recordContextAsFailed(
        context: Context,
        sourceInfo: SourceInfo,
        contextPath: ContextPath,
        exceptionInContext: Throwable
    ) {
        val testDescriptor = TestDescription(context, "error in context", sourceInfo)

        finishedPaths.add(contextPath) // don't visit this context again
        val testPlusResult = TestPlusResult(testDescriptor, Failure(exceptionInContext))
        deferredTestResults[testDescriptor] = CompletableDeferred(testPlusResult)
        foundContexts.add(context)
        staticConfig.listener.testFinished(testPlusResult)
    }
    fun <GivenType> executeTest(
        testDescription: TestDescription,
        function: TestLambda<GivenType>,
        resourcesCloser: ResourcesCloser,
        isolation: Boolean,
        given: suspend () -> GivenType,
        rootContextStartTime: Long
    ) {
        deferredTestResults[testDescription] =
            staticConfig.scope.async(start = staticConfig.coroutineStart) {

                val listener = staticConfig.listener
                listener.testStarted(testDescription)
                val testResult = try {
                    withTimeout(staticConfig.timeoutMillis) {
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
}
