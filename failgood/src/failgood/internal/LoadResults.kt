package failgood.internal

import failgood.*
import failgood.CouldNotLoadTestCollection
import failgood.LoadResult
import failgood.NullExecutionListener
import failgood.internal.execution.TestCollectionExecutor
import failgood.internal.util.getenv
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

// set timeout to the timeout in milliseconds, an empty string to turn it off
private val timeoutMillis: Long = Suite.parseTimeout(getenv("TIMEOUT"))
private val tag = getenv("FAILGOOD_TAG")

internal class LoadResults(private val loadResults: List<LoadResult>) {
    private fun fixRootName(tc: TestCollection<*>) =
        if (tc.addClassName) {
            val shortClassName = tc.sourceInfo.className.substringAfterLast(".")
            // if the root context name is just "root", it is an unnamed context and so
            // we replace it and we change the name and the display name
            val unnamedContext = tc.rootContext.name == "root"

            val newName =
                if (unnamedContext) shortClassName
                else "$shortClassName: ${tc.rootContext.name}"
            if (unnamedContext)
                tc.copy(rootContext = tc.rootContext.copy(displayName = newName, name = newName))
            else
                tc.copy(rootContext = tc.rootContext.copy(displayName = newName))
        } else tc
    fun investigate(
        coroutineScope: CoroutineScope,
        executeTests: Boolean = true,
        executionFilter: TestFilterProvider = ExecuteAllTestFilterProvider,
        listener: ExecutionListener = NullExecutionListener
    ): List<Deferred<TestCollectionExecutionResult>> {
        return loadResults.map { loadResult: LoadResult ->
            when (loadResult) {
                is CouldNotLoadTestCollection ->
                    CompletableDeferred(
                        FailedTestCollectionExecution(
                            Context(loadResult.kClass.simpleName ?: "unknown"),
                            loadResult.reason
                        )
                    )
                is TestCollection<*> -> {
                    val testFilter =
                        loadResult.rootContext.sourceInfo?.className?.let {
                            executionFilter.forClass(it)
                        } ?: ExecuteAllTests
                    coroutineScope.async {
                        if (loadResult.ignored?.isIgnored() == null) {
                            TestCollectionExecutor(
                                    fixRootName(loadResult),
                                    coroutineScope,
                                    !executeTests,
                                    listener,
                                    testFilter,
                                    timeoutMillis,
                                    runOnlyTag = tag
                                )
                                .execute()
                        } else TestResults(emptyList(), mapOf(), setOf())
                    }
                }
            }
        }
    }
}
