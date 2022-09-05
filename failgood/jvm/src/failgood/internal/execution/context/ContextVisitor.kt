package failgood.internal.execution.context

import failgood.*
import failgood.internal.ContextPath
import failgood.internal.ResourcesCloser
import kotlinx.coroutines.CompletableDeferred

internal class ContextVisitor<GivenType>(
    private val staticConfig: StaticContextExecutionConfig,
    private val contextStateCollector: ContextStateCollector,
    private val context: Context,
    // execute sub-contexts and tests regardless of their tags, even when filtering
    private val given: suspend () -> GivenType,
    private val resourcesCloser: ResourcesCloser,
    private val executeAll: Boolean = false,
    // indicate that this context was already executed once, so we already know about all of its tests.
    // there is no need to check tests, just go into sub contexts
    private val onlyRunSubcontexts: Boolean,
    private val rootContextStartTime: Long
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
        if (!executeAll && (staticConfig.runOnlyTag != null && !tags.contains(staticConfig.runOnlyTag)))
            return
        val testPath = ContextPath(context, name)
        if (!staticConfig.testFilter.shouldRun(testPath))
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

            contextStateCollector.executeTest(
                testDescription,
                function,
                resourcesCloser,
                isolation,
                given,
                rootContextStartTime
            )
        } else {
            contextStateCollector.executeTestLater(testDescription, testPath)
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
        if (!executeAll && (staticConfig.runOnlyTag != null && !tags.contains(staticConfig.runOnlyTag)))
            return

        // if we already ran a test in this context we don't need to visit the child context now
        if (this.isolation && ranATest) {
            // but we need to run the root context again to visit this child context
            contextsLeft = true
            if (onlyRunSubcontexts)
                throw ContextFinished()
            return
        }

        if (isolation == false)
            contextStateCollector.containsContextsWithoutIsolation = true

        val contextPath = ContextPath(context, name)
        if (!staticConfig.testFilter.shouldRun(contextPath))
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
                staticConfig,
                contextStateCollector,
                context,
                given,
                resourcesCloser,
                staticConfig.runOnlyTag != null,
                contextStateCollector.investigatedContexts.contains(context),
                rootContextStartTime
            )
        this.mutable = false
        try {
            visitor.mutable = true
            visitor.contextLambda()
            contextStateCollector.investigatedContexts.add(context)
        } catch (_: ContextFinished) {
        } catch (exceptionInContext: ImmutableContextException) {
            // this is fatal, and we treat the whole root context as failed, so we just rethrow
            throw exceptionInContext
        } catch (exceptionInContext: Throwable) {
            contextStateCollector.recordContextAsFailed(context, sourceInfo, contextPath, exceptionInContext)
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
            staticConfig.listener.testFinished(testPlusResult)
        }
    }

    override fun afterSuite(function: suspend () -> Unit) {
        if (!onlyRunSubcontexts)
            contextStateCollector.afterSuiteCallbacks.add(function)
    }
}
