package failgood.internal.execution

import failgood.Context
import failgood.FailGoodException
import failgood.Ignored
import failgood.Skipped
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.dsl.ContextDSL
import failgood.dsl.ContextOnlyResourceDSL
import failgood.dsl.GivenFunction
import failgood.dsl.ResourcesDSL
import failgood.dsl.TestFunction
import failgood.internal.ContextPath
import failgood.internal.ResourcesCloser
import failgood.internal.given.GivenDSLHandler
import kotlinx.coroutines.CompletableDeferred

internal class ContextVisitor<RootGiven, GivenType>(
    private val staticConfig: StaticContextExecutionConfig<RootGiven>,
    private val contextStateCollector: ContextStateCollector<RootGiven>,
    private val context: Context,
    private val resourcesCloser: ResourcesCloser,
    private val executeAll: Boolean = false,
    private val onlyRunSubcontexts: Boolean,
    private val rootContextStartTime: Long,
    private val givenDSL: GivenDSLHandler<GivenType>,
) :
    ContextDSL<GivenType>,
    ResourcesDSL by resourcesCloser,
    ContextOnlyResourceDSL by resourcesCloser {
    private val isolation = context.isolation
    private val namesInThisContext = mutableSetOf<String>()
    private var ranATest = false
    var contextsLeft = false
    private var mutable = true

    override suspend fun it(
        name: String,
        tags: Set<String>,
        ignored: Ignored?,
        function: TestFunction<GivenType>,
    ) {
        if (onlyRunSubcontexts) return

        val ignoreReason = ignored?.isIgnored()
        if (ignoreReason != null) {
            checkForDuplicateName(name)
            val testPath = ContextPath(context, name)
            if (staticConfig.testFilter.shouldRun(testPath) &&
                contextStateCollector.finishedPaths.add(testPath)) {
                val testDescriptor = TestDescription(context, name, sourceInfo())
                val result = Skipped(ignoreReason)
                staticConfig.listener.testDiscovered(testDescriptor)
                val testPlusResult = TestPlusResult(testDescriptor, result)
                contextStateCollector.deferredTestResults[testDescriptor] =
                    CompletableDeferred(testPlusResult)
                staticConfig.listener.testFinished(testPlusResult)
            }
            return
        }
        checkForDuplicateName(name)
        if (!shouldRun(tags)) return
        val testPath = ContextPath(context, name)
        if (!staticConfig.testFilter.shouldRun(testPath)) return
        if (!contextStateCollector.finishedPaths.add(testPath)) return

        val testDescription = TestDescription(context, name, sourceInfo())
        staticConfig.listener.testDiscovered(testDescription)
        withCurrentTestContext(testDescription) {
            if (!ranATest || !isolation) {
                ranATest = true
                contextStateCollector.executeTest(
                    testDescription,
                    function,
                    resourcesCloser,
                    isolation,
                    givenDSL,
                    rootContextStartTime,
                )
            } else {
                contextStateCollector.executeTestLater(testDescription, testPath)
            }
        }
    }

    override suspend fun <ContextDependency> describe(
        name: String,
        tags: Set<String>,
        isolation: Boolean?,
        ignored: Ignored?,
        given: GivenFunction<GivenType, ContextDependency>,
        contextFunction: suspend ContextDSL<ContextDependency>.() -> Unit,
    ) {
        checkForDuplicateName(name)
        if (!shouldRun(tags)) return

        val contextPath = ContextPath(context, name)
        if (!staticConfig.testFilter.shouldRun(contextPath)) return

        if (this.isolation && ranATest) {
            contextsLeft = true
            if (onlyRunSubcontexts) throw ContextFinished()
            return
        }

        if (contextStateCollector.finishedPaths.contains(contextPath)) return
        val subContextShouldHaveIsolation = isolation != false && this.isolation
        val sourceInfo = sourceInfo()
        val context = Context(name, context, sourceInfo, subContextShouldHaveIsolation)
        val ignoreReason = ignored?.isIgnored()
        if (!contextStateCollector.investigatedContexts.contains(context)) {
            staticConfig.listener.contextDiscovered(context)
        }
        if (ignoreReason != null) {
            val testDescriptor =
                TestDescription(context, "context ignored because $ignoreReason", sourceInfo)
            val testPlusResult = TestPlusResult(testDescriptor, Skipped(ignoreReason))
            contextStateCollector.deferredTestResults[testDescriptor] =
                CompletableDeferred(testPlusResult)
            staticConfig.listener.testDiscovered(testDescriptor)
            staticConfig.listener.testFinished(testPlusResult)
            contextStateCollector.finishedPaths.add(contextPath)
            contextStateCollector.investigatedContexts.add(context)
            contextStateCollector.foundContexts.add(context)
            return
        }
        if (isolation == false) contextStateCollector.containsContextsWithoutIsolation = true
        if (isolation == true && !this.isolation) {
            contextStateCollector.recordContextAsFailed(
                context,
                sourceInfo,
                contextPath,
                FailGoodException("in a context without isolation it can not be turned on again"),
            )
            return
        }
        val visitor =
            ContextVisitor(
                staticConfig,
                contextStateCollector,
                context,
                resourcesCloser,
                staticConfig.runOnlyTag != null,
                contextStateCollector.investigatedContexts.contains(context),
                rootContextStartTime,
                givenDSL.add(given),
            )
        this.mutable = false
        try {
            visitor.mutable = true
            visitor.contextFunction()
            contextStateCollector.investigatedContexts.add(context)
        } catch (_: ContextFinished) {} catch (exceptionInContext: ImmutableContextException) {
            throw exceptionInContext
        } catch (exceptionInContext: Throwable) {
            contextStateCollector.recordContextAsFailed(
                context,
                sourceInfo,
                contextPath,
                exceptionInContext,
            )
            ranATest = true
            return
        } finally {
            visitor.mutable = false
            this.mutable = true
        }
        if (visitor.contextsLeft) {
            contextsLeft = true
        } else {
            contextStateCollector.foundContexts.add(context)
            contextStateCollector.finishedPaths.add(contextPath)
        }

        if (visitor.ranATest) ranATest = true
    }

    private fun shouldRun(tags: Set<String>): Boolean {
        return executeAll ||
            (staticConfig.runOnlyTag == null || tags.contains(staticConfig.runOnlyTag))
    }

    private fun checkForDuplicateName(name: String) {
        if (!namesInThisContext.add(name)) {
            throw DuplicateNameInContextException(
                "duplicate name \"$name\" in context \"${context.name}\"")
        }
        if (!mutable) {
            throw ImmutableContextException(
                "Trying to create a test in the wrong context. " +
                    "Make sure functions that create tests have ContextDSL as receiver",
            )
        }
    }

    override fun afterSuite(function: suspend () -> Unit) {
        if (!onlyRunSubcontexts) contextStateCollector.afterSuiteCallbacks.add(function)
    }
}
