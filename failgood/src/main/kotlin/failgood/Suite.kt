package failgood

import failgood.internal.ContextExecutor
import failgood.internal.ContextInfo
import failgood.internal.ContextPath
import failgood.internal.ContextTreeReporter
import failgood.internal.ResourcesCloser
import failgood.internal.SingleTestExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

const val DEFAULT_CONTEXT_TIMEOUT: Long = 20000
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
            this(RootContext("root", false, 0, function))

    fun run(
        parallelism: Int = cpus(),
        silent: Boolean = false
    ): SuiteResult {

        val threadPool =
            if (parallelism > 1)
                Executors.newWorkStealingPool(parallelism)
            else
                Executors.newSingleThreadExecutor()
        return try {
            threadPool.asCoroutineDispatcher()
                .use { dispatcher ->
                    runBlocking(dispatcher) {
                        val contextInfos = findTests(this)
                        if (!silent) {
                            contextInfos.forEach {
                                launch {
                                    val context = it.await()
                                    val contextTreeReporter = ContextTreeReporter()
                                    println(
                                        contextTreeReporter.stringReport(
                                            context.tests.values.awaitAll(),
                                            context.contexts
                                        )
                                            .joinToString("\n")
                                    )
                                }
                            }
                        }
                        val resolvedContexts = contextInfos.awaitAll()
                        val results = resolvedContexts.flatMap { it.tests.values }.awaitAll()
                        resolvedContexts.forEach { it.afterSuiteCallbacks.forEach { callback -> callback.invoke() } }
                        SuiteResult(
                            results,
                            results.filter { it.isFailed },
                            resolvedContexts.flatMap { it.contexts })
                    }
                }
        } finally {
            threadPool.awaitTermination(100, TimeUnit.SECONDS)
            threadPool.shutdown()
        }
    }

    // when timeout is not set use the default.
    // if its set to a number use that as the timeout. if its not a number turn the timeout off
    private val contextTimeout = System.getenv("TIMEOUT").let {
        when (it) {
            null -> DEFAULT_CONTEXT_TIMEOUT
            else -> it.toLongOrNull()
        }
    }

    internal suspend fun findTests(
        coroutineScope: CoroutineScope,
        executeTests: Boolean = true,
        listener: ExecutionListener = NullExecutionListener
    ): List<Deferred<ContextInfo>> {
        return contextProviders
            .map { coroutineScope.async { it.getContexts() } }.flatMap { it.await() }.sortedBy { it.order }
            .map { context: RootContext ->
                coroutineScope.async {
                    yield()
                    if (!context.disabled) {
                        if (contextTimeout != null) {
                            try {
                                withTimeout(contextTimeout) {
                                    ContextExecutor(context, coroutineScope, !executeTests, listener).execute()
                                }
                            } catch (e: TimeoutCancellationException) {
                                throw FailGoodException("context ${context.name} timed out")
                            }
                        } else {
                            ContextExecutor(context, coroutineScope, !executeTests, listener).execute()
                        }
                    } else
                        ContextInfo(emptyList(), mapOf(), setOf())
                }
            }
    }

    fun runSingle(test: String): TestResult {
        val contextName = test.substringBefore(">").trim()
        val context = contextProviders.flatMap { it.getContexts() }.singleOrNull() {
            it.name == contextName
        } ?: throw FailGoodException("No Root context with name $contextName found")
        val desc = ContextPath.fromString(test)
        return runBlocking {
            val resourcesCloser = ResourcesCloser(this)
            SingleTestExecutor(context, desc, object : TestDSL, ResourcesDSL by resourcesCloser {
                override suspend fun println(body: String) {
                    kotlin.io.println(body)
                }

                override suspend fun _test_event(type: String, body: String) {
                    kotlin.io.println(body)
                }
            }, resourcesCloser).execute()
        }
    }
}

object NullExecutionListener : ExecutionListener {
    override suspend fun testStarted(testDescription: TestDescription) {}
    override suspend fun testFinished(testPlusResult: TestPlusResult) {}
    override suspend fun testEvent(testDescription: TestDescription, type: String, payload: String) {}
}

internal fun uptime(totalTests: Int? = null): String {
    val operatingSystemMXBean =
        ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
    val uptime = ManagementFactory.getRuntimeMXBean().uptime
    val cpuTime = operatingSystemMXBean.processCpuTime / 1000000
    val percentage = cpuTime * 100 / uptime
    return "total:${uptime}ms cpu:${cpuTime}ms, load:${percentage}%." + if (totalTests != null) {
        " " + pluralize(totalTests * 1000 / uptime.toInt(), "test") + "/sec"
    } else
        ""
}

internal fun pluralize(count: Int, item: String) = if (count == 1) "1 $item" else "$count ${item}s"
private fun cpus() = Runtime.getRuntime().availableProcessors()
