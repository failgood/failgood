package failgood.internal

import failgood.*
import failgood.internal.execution.TestCollectionExecutor
import failgood.internal.util.StringUniquer
import failgood.internal.util.getenv
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

// set timeout to the timeout in milliseconds, an empty string to turn it off
private val timeoutMillis: Long = Suite.parseTimeout(getenv("TIMEOUT"))
private val tag = getenv("FAILGOOD_TAG")

internal class LoadResults(private val loadResults: List<LoadResult>) {
    private val logger = KotlinLogging.logger {}

    private val testCollectionNameUniquer = StringUniquer()
    private fun fixRootName(tc: TestCollection<*>): TestCollection<out Any?> {
        // if the root context name is just "root", it is an unnamed context and so
        // we replace it and we change the name and the display name
        val name = tc.rootContext.name
        val unnamedContext = name == "root"

        val newDisplayName = if (tc.addClassName) {
            val shortClassName = tc.sourceInfo.className.substringAfterLast(".")

            if (unnamedContext) shortClassName
            else "$shortClassName: $name"
        } else name
        val uniqueNewDisplayName = testCollectionNameUniquer.makeUnique(newDisplayName)
        logger.debug { "uniqueNewDisplayName: $uniqueNewDisplayName" }
        return if (unnamedContext)
            tc.copy(rootContext = tc.rootContext.copy(displayName = uniqueNewDisplayName, name = uniqueNewDisplayName))
        else {
            val uniqueNewName = testCollectionNameUniquer.makeUnique(name)
            logger.debug { "uniqueNewName: $uniqueNewName" }
            tc.copy(rootContext = tc.rootContext.copy(displayName = uniqueNewDisplayName, name= uniqueNewName))
        }
    }

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
                                runOnlyTag = tag
                            )
                                .execute()
                        }
                    } else CompletableDeferred(TestResults(emptyList(), mapOf(), setOf()))
                }
            }
        }
    }
}
