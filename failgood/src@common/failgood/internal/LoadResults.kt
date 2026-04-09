package failgood.internal

import failgood.Context
import failgood.CouldNotLoadTestCollection
import failgood.ExecutionListener
import failgood.LoadResult
import failgood.NullExecutionListener
import failgood.TestCollection
import failgood.internal.execution.TestCollectionExecutor
import failgood.internal.util.StringUniquer
import failgood.internal.util.getenv
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

private val timeoutMillis: Long = parseTimeout(getenv("TIMEOUT"))
private val tag = getenv("FAILGOOD_TAG")

internal class LoadResults(private val loadResults: List<LoadResult>) {
    private val testCollectionNameUniquer = StringUniquer()

    private fun fixRootName(tc: TestCollection<*>): TestCollection<out Any?> {
        val name = tc.rootContext.name
        val unnamedContext = name == "root"

        val newDisplayName =
            if (tc.addClassName) {
                val shortClassName = tc.sourceInfo.className.substringAfterLast(".")
                if (unnamedContext) shortClassName else "$shortClassName: $name"
            } else {
                name
            }
        val uniqueNewDisplayName = testCollectionNameUniquer.makeUnique(newDisplayName)
        return if (unnamedContext) {
            tc.copy(
                rootContext =
                    tc.rootContext.copy(
                        displayName = uniqueNewDisplayName,
                        name = uniqueNewDisplayName,
                    ))
        } else {
            val uniqueNewName = testCollectionNameUniquer.makeUnique(name)
            tc.copy(
                rootContext =
                    tc.rootContext.copy(
                        displayName = uniqueNewDisplayName,
                        name = uniqueNewName,
                    ))
        }
    }

    fun investigate(
        coroutineScope: CoroutineScope,
        executeTests: Boolean = true,
        executionFilter: TestFilterProvider = ExecuteAllTestFilterProvider,
        listener: ExecutionListener = NullExecutionListener,
    ): List<Deferred<TestCollectionExecutionResult>> {
        return loadResults.map { loadResult: LoadResult ->
            when (loadResult) {
                is CouldNotLoadTestCollection ->
                    CompletableDeferred(
                        FailedTestCollectionExecution(
                            Context(loadResult.kClass.simpleName ?: "unknown"),
                            loadResult.reason,
                        ))

                is TestCollection<*> -> {
                    val testFilter =
                        loadResult.rootContext.sourceInfo?.className?.let {
                            executionFilter.forClass(it)
                        } ?: ExecuteAllTests
                    if (loadResult.ignored?.isIgnored() == null) {
                        val testCollection = fixRootName(loadResult)
                        coroutineScope.async {
                            TestCollectionExecutor(
                                    testCollection,
                                    coroutineScope,
                                    !executeTests,
                                    listener,
                                    testFilter,
                                    timeoutMillis,
                                    runOnlyTag = tag,
                                )
                                .execute()
                        }
                    } else {
                        CompletableDeferred(TestResults(emptyList(), mapOf(), setOf()))
                    }
                }
            }
        }
    }
}

private fun parseTimeout(timeout: String?): Long {
    return when (timeout) {
        null -> 40000
        "" -> Long.MAX_VALUE
        else ->
            timeout.toLongOrNull()
                ?: throw failgood.FailGoodException("TIMEOUT must be a number or an empty string")
    }
}
