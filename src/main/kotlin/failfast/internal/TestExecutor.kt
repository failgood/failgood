package failfast.internal

import failfast.ContextDSL
import failfast.ContextLambda
import failfast.FailFastException
import failfast.Failed
import failfast.RootContext
import failfast.Success
import failfast.TestDescriptor
import failfast.TestLambda
import failfast.TestResult

internal class TestExecutor(private val context: RootContext, private val test: TestDescriptor) {
    private val closeables = mutableListOf<AutoCloseable>()
    private var testResult: TestResult? = null
    private val startTime = System.nanoTime()
    suspend fun execute(): TestResult {
        val dsl: ContextDSL = contextDSL(test.parentContext.path.drop(1))
        dsl.(context.function)()
        closeables.forEach { it.close() }
        return testResult ?: throw FailFastException("no test found for test $test")
    }

    open inner class Base : ContextDSL {
        override suspend fun test(name: String, function: TestLambda) {
        }

        override suspend fun context(name: String, function: ContextLambda) {
        }

        override suspend fun describe(name: String, function: ContextLambda) {
        }

        override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T {
            closeables.add(AutoCloseable { closeFunction(wrapped) })
            return wrapped
        }

        override suspend fun it(behaviorDescription: String, function: TestLambda) {
        }

        override fun itWill(behaviorDescription: String, function: TestLambda) {
        }
    }

    inner class ContextFinder(private val contexts: List<String>) : ContextDSL, Base() {
        override suspend fun context(name: String, function: ContextLambda) {
            if (contexts.first() != name) return

            contextDSL(contexts.drop(1)).function()
        }

        override suspend fun describe(name: String, function: ContextLambda) {
            context(name, function)
        }
    }

    private fun contextDSL(parentContexts: List<String>): ContextDSL =
        if (parentContexts.isEmpty()) TestFinder() else ContextFinder(parentContexts)

    inner class TestFinder : Base() {
        override suspend fun it(behaviorDescription: String, function: TestLambda) {
            test(behaviorDescription, function)
        }

        override suspend fun test(name: String, function: TestLambda) {
            if (test.testName == name)
                testResult =
                    try {
                        function()
                        Success(test, (System.nanoTime() - startTime) / 1000)
                    } catch (e: Throwable) {
                        Failed(test, e)
                    }
        }
    }
}
