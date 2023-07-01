package failgood.internal

import failgood.Context
import failgood.CouldNotLoadContext
import failgood.ExecutionListener
import failgood.LoadResult
import failgood.NullExecutionListener
import failgood.RootContext
import failgood.Suite
import failgood.internal.execution.context.ContextExecutor
import failgood.internal.util.getenv
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
// set timeout to the timeout in milliseconds, an empty string to turn it off
private val timeoutMillis: Long = Suite.parseTimeout(getenv("TIMEOUT"))
private val tag = getenv("FAILGOOD_TAG")

internal class LoadResults(internal val loadResults: List<LoadResult>) {
    fun investigate(
        coroutineScope: CoroutineScope,
        executeTests: Boolean = true,
        executionFilter: TestFilterProvider = ExecuteAllTestFilterProvider,
        listener: ExecutionListener = NullExecutionListener
    ): List<Deferred<ContextResult>> {
        return loadResults.map { context: LoadResult ->
            when (context) {
                is CouldNotLoadContext ->
                    CompletableDeferred(
                        FailedRootContext(Context(context.jClass.name ?: "unknown"), context.reason)
                    )
                is RootContext -> {
                    val testFilter = executionFilter.forClass(context.sourceInfo.className)
                    coroutineScope.async {
                        if (context.ignored?.isIgnored() == null) {
                            ContextExecutor(
                                context,
                                coroutineScope,
                                !executeTests,
                                listener,
                                testFilter,
                                timeoutMillis,
                                runOnlyTag = tag
                            ).execute()
                        } else
                            ContextInfo(emptyList(), mapOf(), setOf())
                    }
                }
            }
        }
    }
}
