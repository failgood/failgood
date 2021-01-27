package failfast.internal

import failfast.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

internal class ContextExecutor(
    private val rootContext: RootContext,
    val scope: CoroutineScope,
    val coroutineStart: CoroutineStart = CoroutineStart.DEFAULT
) {
    private val startTime = System.nanoTime()
    private val finishedContexts = ConcurrentHashMap.newKeySet<Context>()!!
    val executedTests = ConcurrentHashMap<TestDescriptor, Deferred<TestResult>>()

    private inner class ContextVisitor(
        private val parentContext: Context,
        private val resourcesCloser: ResourcesCloser
    ) : ContextDSL {
        private val testsInThisContexts = mutableSetOf<String>() // to find duplicates

        // we only run the first new test that we find here. the remaining tests of the context
        // run with the TestExecutor.
        private var ranATest = false
        var contextsLeft = false // are there sub contexts left to run?

        override suspend fun test(name: String, function: TestLambda) {
            if (!testsInThisContexts.add(name))
                throw FailFastException("duplicate name $name in context $parentContext")
            val testDescriptor = TestDescriptor(parentContext, name)
            if (executedTests.containsKey(testDescriptor)) {
                return
            } else if (!ranATest) {
                ranATest = true

                // create the tests stacktrace element outside of the async block to get a better stacktrace
                val stackTraceElement =
                    RuntimeException().stackTrace.first { !(it.fileName?.endsWith("ContextExecutor.kt") ?: true) }
                val deferred =
                    scope.async(start = coroutineStart) {
                        val testResult =
                            try {
                                function()
                                resourcesCloser.close()
                                Success(testDescriptor, (System.nanoTime() - startTime) / 1000)
                            } catch (e: Throwable) {
                                Failed(testDescriptor, e, stackTraceElement.toString())
                            }

                        testResult
                    }
                executedTests[testDescriptor] = deferred
            } else {
                val deferred =
                    scope.async(start = coroutineStart) {
                        SingleTestExecutor(rootContext, testDescriptor).execute()
                    }
                executedTests[testDescriptor] = deferred
            }
        }

        override suspend fun context(name: String, function: ContextLambda) {
            if (!testsInThisContexts.add(name))
                throw FailFastException("duplicate name $name in context $parentContext")
            // if we already ran a test in this context we don't need to visit the child context now
            if (ranATest) {
                contextsLeft = true
                // but we need to run the root context again to visit this child context
                return
            }
            val context = Context(name, parentContext)
            if (finishedContexts.contains(context)) return
            val visitor = ContextVisitor(context, resourcesCloser)
            try {
                visitor.function()
            } catch (e: Exception) {
                val testDescriptor = TestDescriptor(parentContext, name)
                val stackTraceElement =
                    RuntimeException().stackTrace.first { !(it.fileName?.endsWith("ContextExecutor.kt") ?: true) }
                        .toString()

                executedTests[testDescriptor] = CompletableDeferred(Failed(testDescriptor, e, stackTraceElement))
                finishedContexts.add(context)
                ranATest = true
            }
            if (visitor.contextsLeft) contextsLeft = true else finishedContexts.add(context)

            if (visitor.ranATest) ranATest = true
        }

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
            val testDescriptor = TestDescriptor(parentContext, "will $behaviorDescription")
            @Suppress("DeferredResultUnused")
            executedTests.computeIfAbsent(testDescriptor) {
                CompletableDeferred(Ignored(testDescriptor))
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
        finishedContexts.add(rootContext)
        return ContextInfo(finishedContexts, executedTests)
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
