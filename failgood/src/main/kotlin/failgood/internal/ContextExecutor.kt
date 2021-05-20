package failgood.internal

import failgood.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

internal class ContextExecutor(
    private val rootContext: RootContext,
    val scope: CoroutineScope,
    lazy: Boolean = false,
    val listener: ExecutionListener = NullExecutionListener
) {
    val coroutineStart: CoroutineStart = if (lazy) CoroutineStart.LAZY else CoroutineStart.DEFAULT
    private var startTime = System.nanoTime()

    private val foundContexts = mutableListOf<Context>()
    private val deferredTestResults = LinkedHashMap<TestDescription, Deferred<TestPlusResult>>()
    private val processedTests = LinkedHashSet<ContextPath>()

    private inner class ContextVisitor(
        private val parentContext: Context,
        private val resourcesCloser: ResourcesCloser
    ) : ContextDSL, ResourcesDSL by resourcesCloser {
        private val namesInThisContext = mutableSetOf<String>() // test and context names to detect duplicates

        // we only run the first new test that we find here. the remaining tests of the context
        // run with the TestExecutor.
        private var ranATest = false
        var contextsLeft = false // are there sub contexts left to run?

        override suspend fun test(name: String, function: TestLambda) {
            if (!namesInThisContext.add(name))
                throw FailGoodException("duplicate name $name in context $parentContext")
            val testPath = ContextPath(parentContext, name)
            // we process each test only once
            if (!processedTests.add(testPath)) {
                return
            }
            val stackTraceElement = getStackTraceElement()
            val testDescription = TestDescription(parentContext, name, stackTraceElement)
            if (!ranATest) {
                // we did not yet run a test so we are going to run this test ourselves
                ranATest = true

                // create the tests stacktrace element outside of the async block to get a better stacktrace#
                val deferred =
                    scope.async(start = coroutineStart) {
                        listener.testStarted(testDescription)
                        val testResult =
                            try {
                                TestContext(resourcesCloser, listener, testDescription).function()
                                resourcesCloser.close()
                                Success((System.nanoTime() - startTime) / 1000)
                            } catch (e: Throwable) {
                                Failed(e)
                            }
                        val testPlusResult = TestPlusResult(testDescription, testResult)
                        listener.testFinished(testPlusResult)
                        testPlusResult
                    }
                deferredTestResults[testDescription] = deferred
            } else {
                val resourcesCloser = ResourcesCloser(scope)
                val deferred =
                    scope.async(start = coroutineStart) {
                        listener.testStarted(testDescription)
                        val result =
                            SingleTestExecutor(
                                rootContext,
                                testPath,
                                TestContext(resourcesCloser, listener, testDescription),
                                resourcesCloser
                            ).execute()
                        val testPlusResult = TestPlusResult(testDescription, result)
                        listener.testFinished(testPlusResult)
                        testPlusResult
                    }
                deferredTestResults[testDescription] = deferred
            }
        }


        override suspend fun context(name: String, function: ContextLambda) {
            if (!namesInThisContext.add(name))
                throw FailGoodException("duplicate name $name in context $parentContext")
            // if we already ran a test in this context we don't need to visit the child context now
            if (ranATest) {
                contextsLeft = true
                // but we need to run the root context again to visit this child context
                return
            }
            val contextPath = ContextPath(parentContext, name)
            if (processedTests.contains(contextPath)) return
            val stackTraceElement = getStackTraceElement()
            val context = Context(name, parentContext, stackTraceElement)
            val visitor = ContextVisitor(context, resourcesCloser)
            try {
                visitor.function()
            } catch (exceptionInContext: Throwable) {
                val testDescriptor = TestDescription(parentContext, name, stackTraceElement)

                processedTests.add(contextPath) // don't visit this context again
                val testPlusResult = TestPlusResult(testDescriptor, Failed(exceptionInContext))
                deferredTestResults[testDescriptor] =
                    CompletableDeferred(testPlusResult)
                listener.testFinished(testPlusResult)
                ranATest = true
                return
            }
            if (visitor.contextsLeft) {
                contextsLeft = true
            } else {
                foundContexts.add(context)
                processedTests.add(contextPath)
            }
            getStackTraceElement().lineNumber

            if (visitor.ranATest) ranATest = true
        }

        private fun getStackTraceElement() =
            RuntimeException().stackTrace.first { !(it.fileName?.endsWith("ContextExecutor.kt") ?: true) }!!

        override suspend fun describe(name: String, function: ContextLambda) {
            context(name, function)
        }


        override suspend fun it(behaviorDescription: String, function: TestLambda) {
            test(behaviorDescription, function)
        }

        override suspend fun pending(behaviorDescription: String, function: TestLambda) {
            val testPath = ContextPath(parentContext, behaviorDescription)

            if (processedTests.add(testPath)) {
                val testDescriptor = TestDescription(parentContext, behaviorDescription, getStackTraceElement())
                val result = Pending

                @Suppress("DeferredResultUnused")
                val testPlusResult = TestPlusResult(testDescriptor, result)
                deferredTestResults[testDescriptor] = CompletableDeferred(testPlusResult)
                listener.testFinished(testPlusResult)
            }
        }
    }

    suspend fun execute(): ContextInfo {
        val function = rootContext.function
        val rootContext = Context(rootContext.name, null, rootContext.stackTraceElement)
        while (true) {
            startTime = System.nanoTime()
            val resourcesCloser = ResourcesCloser(scope)
            val visitor = ContextVisitor(rootContext, resourcesCloser)
            visitor.function()
            if (!visitor.contextsLeft) break
        }
        // context order: first root context, then sub-contexts ordered by line number
        val contexts = listOf(rootContext) + foundContexts.sortedBy { it.stackTraceElement!!.lineNumber }
        return ContextInfo(contexts, deferredTestResults)
    }
}

class ResourcesCloser(private val scope: CoroutineScope) : ResourcesDSL {
    override fun <T> autoClose(wrapped: T, closeFunction: suspend (T) -> Unit): T {
        add(SuspendAutoCloseable(wrapped, closeFunction))
        return wrapped
    }

    override suspend fun <T> dependency(creator: suspend () -> T, closer: suspend (T) -> Unit): TestDependency<T> {
        val result = scope.async(Dispatchers.IO) { creator() }
        add(SuspendAutoCloseable(result) { closer(result.await()) })
        return TestDependency(result)
    }

    override fun <T : AutoCloseable> autoClose(wrapped: T): T = autoClose(wrapped) { it.close() }

    private fun <T> add(autoCloseable: SuspendAutoCloseable<T>) {
        closeables.add(autoCloseable)
    }

    suspend fun close() {
        closeables.reversed().forEach { it.close() }
    }

    private val closeables = ConcurrentLinkedQueue<SuspendAutoCloseable<*>>()
}

class SuspendAutoCloseable<T>(private val wrapped: T, val closer: suspend (T) -> Unit) {
    suspend fun close() {
        closer(wrapped)
    }
}
