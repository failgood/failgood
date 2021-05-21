package failgood.internal

import failgood.*

/**
 * Executes a single test with all its parent contexts
 * Async Called by ContextExecutor to execute all tests that it does not have to execute itself
 */
internal class SingleTestExecutor(
    private val context: RootContext,
    private val test: ContextPath,
    val testDSL: TestDSL,
    val resourcesCloser: ResourcesCloser
) {
    private val startTime = System.nanoTime()
    suspend fun execute(): TestResult {
        val dsl: ContextDSL = contextDSL(test.parentContext.path.drop(1))
        return try {
            dsl.(context.function)()
            throw FailGoodException("specified test not found: $test")
        } catch (e: TestResultAvailable) {
            e.testResult
        } catch (e: Throwable) {
            Failed(e)
        }
    }

    open inner class Base : ContextDSL, ResourcesDSL by resourcesCloser {
        override suspend fun test(name: String, function: TestLambda) {
        }

        override suspend fun context(name: String, function: ContextLambda) {
        }

        override suspend fun describe(name: String, function: ContextLambda) {
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
                        testDSL.function()
                        resourcesCloser.close()
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
