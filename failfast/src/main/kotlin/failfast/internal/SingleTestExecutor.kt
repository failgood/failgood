package failfast.internal

import failfast.ContextDSL
import failfast.ContextLambda
import failfast.ContextPath
import failfast.FailFastException
import failfast.Failed
import failfast.RootContext
import failfast.Success
import failfast.TestLambda
import failfast.TestResult

/**
 * Executes a single test with all its parent contexts
 * Async Called by ContextExecutor to execute all tests that it does not have to execute itself
 */
internal class SingleTestExecutor(private val context: RootContext, private val test: ContextPath) {
    private val closeables = mutableListOf<SuspendAutoCloseable<*>>()
    private val startTime = System.nanoTime()
    suspend fun execute(): TestResult {
        val dsl: ContextDSL = contextDSL(test.parentContext.path.drop(1))
        val testResult = try {
            dsl.(context.function)()
            throw FailFastException("specified test not found: $test")
        } catch (e: TestResultAvailable) {
            e.testResult
        } catch (e: Throwable) {
            Failed(e)
        }
        try {
            closeables.forEach { it.close() }
        } catch (e: Throwable) {
            return Failed(e)
        }
        return testResult
    }

    open inner class Base : ContextDSL {
        override suspend fun test(name: String, function: TestLambda) {
        }

        override suspend fun context(name: String, function: ContextLambda) {
        }

        override suspend fun describe(name: String, function: ContextLambda) {
        }

        override fun <T> autoClose(wrapped: T, closeFunction: suspend (T) -> Unit): T {
            closeables.add(SuspendAutoCloseable(wrapped, closeFunction))
            return wrapped
        }

        override suspend fun it(behaviorDescription: String, function: TestLambda) {}

        override suspend fun pending(behaviorDescription: String, function: TestLambda) {}
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
            if (test.name == name) {
                throw TestResultAvailable(
                    try {
                        function()
                        Success((System.nanoTime() - startTime) / 1000)
                    } catch (e: Throwable) {
                        Failed(e)
                    }
                )
            }
        }
    }

    private class TestResultAvailable(val testResult: TestResult) : RuntimeException()
}
