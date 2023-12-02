package failgood.internal.execution.context

import failgood.*
import failgood.internal.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.withTimeout

internal class ContextExecutor(
    private val rootContext: RootContext,
    scope: CoroutineScope,
    lazy: Boolean = false,
    listener: ExecutionListener = NullExecutionListener,
    testFilter: TestFilter = ExecuteAllTests,
    timeoutMillis: Long = 40000L,
    runOnlyTag: String? = null
) {
    private val staticExecutionConfig =
        StaticContextExecutionConfig(
            rootContext.function,
            scope,
            listener,
            testFilter,
            timeoutMillis,
            if (lazy) CoroutineStart.LAZY else CoroutineStart.DEFAULT,
            runOnlyTag
        )

    private val stateCollector =
        ContextStateCollector(staticExecutionConfig, !rootContext.context.isolation)

    /**
     * Execute the rootContext.
     *
     * We keep executing the Context DSL until we know about all contexts and tests in this root
     * context The first test in each context is directly executed (async via coroutines), and for
     * all other tests in that context we create a SingleTestExecutor that executes the whole
     * context path of that test together with the test.
     */
    suspend fun execute(): ContextResult {
        val incomingRootContext =
            if (rootContext.addClassName) {
                val shortClassName = rootContext.sourceInfo.className.substringAfterLast(".")
                val newName =
                    if (rootContext.context.name.isNotBlank())
                        "$shortClassName: ${rootContext.context.name}"
                    else shortClassName
                rootContext.copy(context = rootContext.context.copy(displayName = newName))
            } else rootContext
        if (!staticExecutionConfig.testFilter.shouldRun(incomingRootContext))
            return ContextInfo(listOf(), mapOf(), setOf())
        val function = rootContext.function
        val rootContext = incomingRootContext.context
        staticExecutionConfig.listener.contextDiscovered(rootContext)
        try {
            do {
                val startTime = System.nanoTime()
                val resourcesCloser = ResourceCloserImpl(staticExecutionConfig.scope)
                val visitor =
                    ContextVisitor<Unit, Unit>(
                        staticExecutionConfig,
                        stateCollector,
                        rootContext,
                        {},
                        resourcesCloser,
                        false,
                        stateCollector.investigatedContexts.contains(rootContext),
                        startTime
                    )
                try {
                    withTimeout(staticExecutionConfig.timeoutMillis) { visitor.function() }
                } catch (_: ContextFinished) {}
                stateCollector.investigatedContexts.add(rootContext)
                if (stateCollector.containsContextsWithoutIsolation) {
                    stateCollector.afterSuiteCallbacks.add { resourcesCloser.closeAutoCloseables() }
                }
            } while (visitor.contextsLeft)
        } catch (e: Throwable) {
            return FailedRootContext(rootContext, e)
        }
        // context order: first root context, then sub-contexts ordered by line number
        val contexts =
            listOf(rootContext) +
                stateCollector.foundContexts.sortedBy { it.sourceInfo!!.lineNumber }
        return ContextInfo(
            contexts,
            stateCollector.deferredTestResults,
            stateCollector.afterSuiteCallbacks
        )
    }
}

// this is thrown when a context is finished, and we can not do anything meaningful in this pass
class ContextFinished : DSLGotoException()

fun sourceInfo(): SourceInfo {
    // find the first stack trace element that is not in this class or ContextDSL
    // (ContextDSL because of default parameters defined there)
    val first =
        RuntimeException().stackTrace.first {
            !(it.fileName?.let { fileName ->
                fileName.endsWith("ContextVisitor.kt") ||
                    fileName.endsWith("ContextExecutor.kt") ||
                    fileName.endsWith("ContextDSL.kt")
            } ?: true)
        }
    return first.let { SourceInfo(it.className, it.fileName!!, it.lineNumber) }
}

internal class DuplicateNameInContextException(s: String) : FailGoodException(s)

internal class ImmutableContextException(s: String) : FailGoodException(s)
