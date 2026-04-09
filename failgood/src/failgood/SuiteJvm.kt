package failgood

import failgood.internal.ContextTreeReporter
import failgood.internal.FailedTestCollectionExecution
import failgood.internal.SuiteExecutionContext
import failgood.internal.TestCollectionExecutionResult
import failgood.internal.TestFilterProvider
import failgood.internal.TestResults
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext

internal actual fun runSuiteBlocking(
    suite: Suite,
    parallelism: Int?,
    silent: Boolean,
    filter: TestFilterProvider,
    listener: ExecutionListener
): SuiteResult {
    return SuiteExecutionContext(parallelism).use { suiteExecutionContext ->
        if (!silent) {
            println("starting test suite with parallelism = ${suiteExecutionContext.parallelism}")
        }
        suiteExecutionContext.coroutineDispatcher.use { dispatcher ->
            runBlocking(dispatcher + MDCContext()) {
                val contextInfos =
                    suite.findAndStartTests(this, filter = filter, listener = listener)
                if (!silent) {
                    printResults(this, contextInfos)
                }
                awaitTestResults(contextInfos.awaitAll())
            }
        }
    }
}

internal fun printResults(
    coroutineScope: CoroutineScope,
    contextInfos: List<Deferred<TestCollectionExecutionResult>>
) {
    contextInfos.forEach {
        coroutineScope.launch {
            val context = it.await()
            val contextTreeReporter = ContextTreeReporter()
            when (context) {
                is TestResults -> {
                    println(
                        contextTreeReporter
                            .stringReport(context.tests.values.awaitAll(), context.contexts)
                            .joinToString("\n"))
                }

                is FailedTestCollectionExecution -> {
                    println(
                        "context ${context.context} failed: ${context.failure.stackTraceToString()}")
                }
            }
        }
    }
}

fun Suite(kClasses: List<KClass<*>>): Suite = Suite(kClasses.map { ObjectContextProvider(it) })
