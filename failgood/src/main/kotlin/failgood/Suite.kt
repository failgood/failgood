package failgood

import failgood.internal.ContextExecutor
import failgood.internal.ContextInfo
import failgood.internal.ContextPath
import failgood.internal.ContextResult
import failgood.internal.ContextTreeReporter
import failgood.internal.ExecuteAllTestFilterProvider
import failgood.internal.FailedContext
import failgood.internal.ResourcesCloser
import failgood.internal.SingleTestExecutor
import failgood.internal.TestFilterProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.lang.management.ManagementFactory
import kotlin.reflect.KClass
import kotlin.system.exitProcess

const val DEFAULT_TIMEOUT: Long = 40000

class Suite(val contextProviders: Collection<ContextProvider>) {
    companion object {
        fun fromContexts(rootContexts: Collection<RootContext>) =
            Suite(rootContexts.map { ContextProvider { listOf(it) } })

        fun fromClasses(classes: List<KClass<*>>) =
            Suite(classes.map { ObjectContextProvider(it) })
    }

    init {
        if (contextProviders.isEmpty()) throw EmptySuiteException()
    }

    constructor(rootContext: RootContext) :
        this(listOf(ContextProvider { listOf(rootContext) }))

    constructor(function: ContextLambda) :
        this(RootContext("root", false, 0, function = function))

    @OptIn(ExperimentalCoroutinesApi::class)
    fun run(
        parallelism: Int? = null,
        silent: Boolean = false
    ): SuiteResult {
        val dispatcher =
            if (parallelism == null) Dispatchers.Default
            else Dispatchers.Default.limitedParallelism(parallelism)
        return runBlocking(dispatcher) {
            val contextInfos = findTests(this)
            awaitTestResult(this, silent, contextInfos)
        }
    }

    private suspend fun awaitTestResult(
        coroutineScope: CoroutineScope,
        silent: Boolean,
        contextInfos: List<FoundContext>
    ): SuiteResult {
        if (!silent) {
            printResults(coroutineScope, contextInfos)
        }
        val resolvedContexts = contextInfos.map { it.result }.awaitAll()
        val successfulContexts = resolvedContexts.filterIsInstance<ContextInfo>()
        val results = successfulContexts.flatMap { it.tests.values }.awaitAll()
        successfulContexts.forEach {
            it.afterSuiteCallbacks.forEach { callback ->
                try {
                    callback.invoke()
                } catch (ignored: Exception) {
                } catch (ignored: AssertionError) {
                }
            }
        }
        return SuiteResult(
            results,
            results.filter { it.isFailed },
            successfulContexts.flatMap { it.contexts }
        )
    }

    private fun printResults(coroutineScope: CoroutineScope, contextInfos: List<FoundContext>) {
        contextInfos.forEach {
            coroutineScope.launch {
                val context = try {
                    withTimeout(this@Suite.timeoutMillis) { it.result.await() }
                } catch (e: CancellationException) {
                    println("context ${it.context.name} never finished")
                    exitProcess(-1)
                }
                val contextTreeReporter = ContextTreeReporter()
                when (context) {
                    is ContextInfo -> {
                        println(
                            contextTreeReporter.stringReport(
                                context.tests.values.awaitAll(),
                                context.contexts
                            )
                                .joinToString("\n")
                        )
                    }
                    is FailedContext -> {
                        println("context ${context.context} failed: ${context.failure.stackTraceToString()}")
                    }
                }
            }
        }
    }

    // set timeout to the timeout in milliseconds, an empty string to turn it off
    private val timeoutMillis: Long = System.getenv("TIMEOUT").let {
        when (it) {
            null -> DEFAULT_TIMEOUT
            "" -> null
            else -> it.toLongOrNull() ?: throw FailGoodException("TIMEOUT must be a number or an empty string")
        }
    } ?: Long.MAX_VALUE

    data class FoundContext(val context: RootContext, val result: Deferred<ContextResult>)
    internal suspend fun findTests(
        coroutineScope: CoroutineScope,
        executeTests: Boolean = true,
        executionFilter: TestFilterProvider = ExecuteAllTestFilterProvider,
        listener: ExecutionListener = NullExecutionListener
    ): List<FoundContext> {
        return contextProviders
            .map { coroutineScope.async { it.getContexts() } }.flatMap { it.await() }.sortedBy { it.order }
            .map { context: RootContext ->
                val testFilter = executionFilter.forClass(context.sourceInfo.className)
                FoundContext(
                    context,
                    coroutineScope.async {
                        if (!context.disabled) {
                            ContextExecutor(
                                context,
                                coroutineScope,
                                !executeTests,
                                listener,
                                testFilter,
                                timeoutMillis
                            ).execute()
                        } else
                            ContextInfo(emptyList(), mapOf(), setOf())
                    }
                )
            }
    }

    fun runSingle(test: String): TestResult {
        val contextName = test.substringBefore(">").trim()
        val context = contextProviders.flatMap { it.getContexts() }.singleOrNull {
            it.name == contextName
        } ?: throw FailGoodException("No Root context with name $contextName found")
        val desc = ContextPath.fromString(test)
        return runBlocking {
            val resourcesCloser = ResourcesCloser(this)
            SingleTestExecutor(
                context, desc,
                object : TestDSL, ResourcesDSL by resourcesCloser {
                    override suspend fun println(body: String) {
                        kotlin.io.println(body)
                    }

                    override suspend fun _test_event(type: String, body: String) {
                        kotlin.io.println(body)
                    }
                },
                resourcesCloser
            ).execute()
        }
    }
}

object NullExecutionListener : ExecutionListener {
    override suspend fun testStarted(testDescription: TestDescription) {}
    override suspend fun testFinished(testPlusResult: TestPlusResult) {}
    override suspend fun testEvent(testDescription: TestDescription, type: String, payload: String) {}
}

private val operatingSystemMXBean =
    ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
private val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
internal fun upt(): Long = runtimeMXBean.uptime

internal fun uptime(totalTests: Int? = null): String {
    val uptime = upt()
    val cpuTime = operatingSystemMXBean.processCpuTime / 1000000
    val percentage = cpuTime * 100 / uptime
    return "total:${uptime}ms cpu:${cpuTime}ms, load:$percentage%." + if (totalTests != null) {
        " " + pluralize(totalTests * 1000 / uptime.toInt(), "test") + "/sec"
    } else
        ""
}

internal fun pluralize(count: Int, item: String) = if (count == 1) "1 $item" else "$count ${item}s"
