package failfast.internal

import failfast.Context
import failfast.ContextDSL
import failfast.ContextInfo
import failfast.ContextLambda
import failfast.Failed
import failfast.Ignored
import failfast.RootContext
import failfast.Success
import failfast.TestDescriptor
import failfast.TestLambda
import failfast.TestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

internal class ContextExecutor(
    private val rootContext: RootContext,
    val testResultChannel: Channel<TestResult>,
    val scope: CoroutineScope
) {

    private val finishedContexts = ConcurrentHashMap.newKeySet<Context>()!!
    val executedTests = ConcurrentHashMap.newKeySet<TestDescriptor>()!!


    private inner class ContextVisitor(
        private val parentContext: Context,
        private val resourcesCloser: ResourcesCloser
    ) :
        ContextDSL {
        private var ranATest =
            false // we only run one test per instance so if this is true we don't invoke test lambdas
        var moreTestsLeft = false // are there more tests left to run?

        override suspend fun test(name: String, function: TestLambda) {
            val testDescriptor = TestDescriptor(parentContext, name)
            if (executedTests.contains(testDescriptor)) {
                return
            } else if (!ranATest) {
                ranATest = true
                executedTests.add(testDescriptor)
                scope.launch {
                    val testResult = try {
                        function()
                        resourcesCloser.close()
                        Success(testDescriptor)
                    } catch (e: AssertionError) {
                        Failed(testDescriptor, e)
                    }
                    testResultChannel.send(testResult)
                }

            } else {
                moreTestsLeft = true
            }
        }

        override suspend fun test(ignoredTestName: String) {
            val testDescriptor = TestDescriptor(parentContext, ignoredTestName)
            if (executedTests.add(testDescriptor))
                testResultChannel.send(Ignored(testDescriptor))
        }

        override suspend fun context(name: String, function: ContextLambda) {
            if (ranATest) { // if we already ran a test in this context we don't need to visit the child context now
                moreTestsLeft = true // but we need to run the root context again to visit this child context
                return
            }
            val context = Context(name, parentContext)
            if (finishedContexts.contains(context))
                return
            val visitor = ContextVisitor(context, resourcesCloser)
            visitor.function()
            if (visitor.moreTestsLeft)
                moreTestsLeft = true
            else
                finishedContexts.add(context)

            if (visitor.ranATest)
                ranATest = true
        }

        override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T {
            return wrapped.apply { resourcesCloser.add { closeFunction(wrapped) } }
        }

        override suspend fun it(behaviorDescription: String, function: TestLambda) {
            test(behaviorDescription, function)
        }

        override suspend fun itWill(behaviorDescription: String) {
            test(behaviorDescription)
        }

        override suspend fun itWill(behaviorDescription: String, function: TestLambda) {
            test(behaviorDescription)
        }
    }

    suspend fun execute(): ContextInfo {
        val function = rootContext.function
        val rootContext = Context(rootContext.name, null)
        while (true) {
            val resourcesCloser = ResourcesCloser()
            val visitor = ContextVisitor(rootContext, resourcesCloser)
            visitor.function()
            if (!visitor.moreTestsLeft)
                break
        }
        finishedContexts.add(rootContext)
        return ContextInfo(finishedContexts, executedTests.size)
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
