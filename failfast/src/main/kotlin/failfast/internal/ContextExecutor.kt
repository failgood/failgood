package failfast.internal

import failfast.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

internal class ContextExecutor(
    private val rootContext: RootContext,
    val scope: CoroutineScope,
    lazy: Boolean = false,
    val listener: ExecutionListener = NullExecutionListener
) {
    val coroutineStart: CoroutineStart = if (lazy) CoroutineStart.LAZY else CoroutineStart.DEFAULT
    private val startTime = System.nanoTime()

    private val foundContexts = mutableListOf<Context>()
    private val deferredTestResults = LinkedHashMap<TestDescription, Deferred<TestResult>>()
    private val processedTests = LinkedHashSet<ContextPath>()

    private inner class ContextVisitor(
        private val parentContext: Context,
        private val resourcesCloser: ResourcesCloser
    ) : ContextDSL {
        private val namesInThisContext = mutableSetOf<String>() // test and context names to detect duplicates

        // we only run the first new test that we find here. the remaining tests of the context
        // run with the TestExecutor.
        private var ranATest = false
        var contextsLeft = false // are there sub contexts left to run?

        override suspend fun test(name: String, function: TestLambda) {
            if (!namesInThisContext.add(name))
                throw FailFastException("duplicate name $name in context $parentContext")
            val testPath = ContextPath(parentContext, name)
            // we process each test only once
            if (!processedTests.add(testPath)) {
                return
            }
            val stackTraceElement = getStackTraceElement()
            val testDescriptor = TestDescription(parentContext, name, stackTraceElement)
            if (!ranATest) {
                // we did not yet run a test so we are going to run this test ourselves
                ranATest = true

                // create the tests stacktrace element outside of the async block to get a better stacktrace
                val deferred =
                    scope.async(start = coroutineStart) {
                        listener.testStarted(testDescriptor)
                        val testResult =
                            try {
                                function()
                                resourcesCloser.close()
                                Success(testDescriptor, (System.nanoTime() - startTime) / 1000)
                            } catch (e: Throwable) {
                                Failed(testDescriptor, e)
                            }
                        listener.testFinished(testDescriptor, testResult)
                        testResult
                    }
                deferredTestResults[testDescriptor] = deferred
            } else {
                val deferred =
                    scope.async(start = coroutineStart) {
                        listener.testStarted(testDescriptor)
                        val result = SingleTestExecutor(rootContext, testPath).execute()
                        listener.testFinished(testDescriptor, result)
                        result
                    }
                deferredTestResults[testDescriptor] = deferred
            }
        }


        override suspend fun context(name: String, function: ContextLambda) {
            if (!namesInThisContext.add(name))
                throw FailFastException("duplicate name $name in context $parentContext")
            // if we already ran a test in this context we don't need to visit the child context now
            if (ranATest) {
                contextsLeft = true
                // but we need to run the root context again to visit this child context
                return
            }
            val context = Context(name, parentContext)
            val contextPath = ContextPath(parentContext, name)
            if (processedTests.contains(contextPath)) return
            val visitor = ContextVisitor(context, resourcesCloser)
            try {
                visitor.function()
            } catch (e: Exception) {
                val stackTraceElement = getStackTraceElement()
                val testDescriptor = TestDescription(parentContext, name, stackTraceElement)

                processedTests.add(contextPath) // don't visit this context again
                deferredTestResults[testDescriptor] = CompletableDeferred(Failed(testDescriptor, e))
                ranATest = true
                return
            }
            if (visitor.contextsLeft) {
                contextsLeft = true
            } else {
                foundContexts.add(context.copy(stackTraceElement = getStackTraceElement()))
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

        override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T {
            return wrapped.apply { resourcesCloser.add { closeFunction(wrapped) } }
        }

        override suspend fun it(behaviorDescription: String, function: TestLambda) {
            test(behaviorDescription, function)
        }

        override fun itWill(behaviorDescription: String, function: TestLambda) {
            val testPath = ContextPath(parentContext, behaviorDescription)

            if (processedTests.add(testPath)) {
                val testDescriptor = TestDescription(parentContext, "will $behaviorDescription", getStackTraceElement())
                @Suppress("DeferredResultUnused")
                deferredTestResults[testDescriptor] = CompletableDeferred(Ignored(testDescriptor))

            }
        }
    }

    suspend fun execute(): ContextInfo {
        val function = rootContext.function
        val rootContext = Context(rootContext.name, null)
        while (true) {
            val resourcesCloser = ResourcesCloser()
            val visitor = ContextVisitor(rootContext, resourcesCloser)
            visitor.function()
            if (!visitor.contextsLeft) break
        }
        // contexts: root context, subcontexts ordered by line number, minus failed contexts (those are reported as tests)
        val contexts = listOf(rootContext) + foundContexts.sortedBy { it.stackTraceElement!!.lineNumber }
        return ContextInfo(contexts, deferredTestResults)
    }
}

private class ResourcesCloser {
    fun add(autoCloseable: AutoCloseable) {
        closeables.add(autoCloseable)
    }

    fun close() {
        closeables.forEach { it.close() }
    }

    private val closeables = ConcurrentLinkedQueue<AutoCloseable>()
}
