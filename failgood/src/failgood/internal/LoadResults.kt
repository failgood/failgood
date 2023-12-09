package failgood.internal

import failgood.*
import failgood.CouldNotLoadContext
import failgood.LoadResult
import failgood.NullExecutionListener
import failgood.internal.execution.context.ContextExecutor
import failgood.internal.util.getenv
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

// set timeout to the timeout in milliseconds, an empty string to turn it off
private val timeoutMillis: Long = Suite.parseTimeout(getenv("TIMEOUT"))
private val tag = getenv("FAILGOOD_TAG")

internal class LoadResults(private val loadResults: List<ContextCreator>) {
    fun investigate(
        coroutineScope: CoroutineScope,
        executeTests: Boolean = true,
        executionFilter: TestFilterProvider = ExecuteAllTestFilterProvider,
        listener: ExecutionListener = NullExecutionListener
    ): List<Deferred<ContextResult>> {
        return loadResults.flatMap { it.getContexts() }.map { loadResult: LoadResult ->
            when (loadResult) {
                is CouldNotLoadContext ->
                    CompletableDeferred(
                        FailedRootContext(
                            Context(loadResult.kClass.simpleName ?: "unknown"),
                            loadResult.reason
                        )
                    )
                is RootContextWithGiven<*> -> {
                    val testFilter =
                        loadResult.context.sourceInfo?.className?.let {
                            executionFilter.forClass(it)
                        } ?: ExecuteAllTests
                    coroutineScope.async {
                        if (loadResult.ignored?.isIgnored() == null) {
                            ContextExecutor(
                                    loadResult,
                                    coroutineScope,
                                    !executeTests,
                                    listener,
                                    testFilter,
                                    timeoutMillis,
                                    runOnlyTag = tag
                                )
                                .execute()
                        } else ContextInfo(emptyList(), mapOf(), setOf())
                    }
                }
            }
        }
    }
}
