package failgood.internal

import failgood.*
import failgood.Failed
import failgood.Success

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
        val dsl: ContextDSL<Unit> = contextDSL(test.container.path.drop(1))
        return try {
            dsl.(context.function)()
            throw FailGoodException(
                "test not found: $test.\n" +
                    "please make sure your test names contain no random parts"
            )
        } catch (e: TestResultAvailable) {
            e.testResult
        } catch (e: Throwable) {
            Failed(e)
        }
    }

    private open inner class Base<GivenType> : ContextDSL<GivenType>, ResourcesDSL by resourcesCloser {
        override suspend fun test(name: String, tags: Set<String>, function: GivenTestLambda<GivenType>) {}
        override suspend fun <ContextDependency> context(
            contextName: String,
            tags: Set<String>,
            given: (suspend () -> ContextDependency)?,
            givenTeardown: (suspend (ContextDependency) -> Unit)?,
            contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
        ) {
        }

        override suspend fun context(name: String, tags: Set<String>, function: ContextLambda) {}
        override suspend fun describe(name: String, tags: Set<String>, function: ContextLambda) {}
        override suspend fun it(behaviorDescription: String, tags: Set<String>, function: GivenTestLambda<GivenType>) {}
        override suspend fun pending(behaviorDescription: String, function: TestLambda) {}
        override fun afterSuite(function: suspend () -> Unit) {}
    }

    private inner class ContextFinder<GivenType>(private val contexts: List<String>) : ContextDSL<GivenType>, Base<GivenType>() {
        override suspend fun context(name: String, tags: Set<String>, function: ContextLambda) {
            if (contexts.first() != name) return

            contextDSL(contexts.drop(1)).function()
        }

        override suspend fun describe(name: String, tags: Set<String>, function: ContextLambda) {
            context(name, function = function)
        }
    }

    private fun contextDSL(parentContexts: List<String>): ContextDSL<Unit> =
        if (parentContexts.isEmpty()) TestFinder() else ContextFinder(parentContexts)

    private inner class TestFinder<GivenType> : Base<GivenType>() {
        override suspend fun it(behaviorDescription: String, tags: Set<String>, function: GivenTestLambda<GivenType>) {
            test(behaviorDescription, function = function)
        }

        override suspend fun test(name: String, tags: Set<String>, function: GivenTestLambda<GivenType>) {
            if (test.name == name) {
                throw TestResultAvailable(executeTest(function))
            }
        }

        private suspend fun executeTest(function: GivenTestLambda<GivenType>): TestResult {
            try {
                testDSL.function(null as GivenType) // TODO given
            } catch (e: Throwable) {
                resourcesCloser.close()
                return Failed(e)
            }
            resourcesCloser.close()
            return Success((System.nanoTime() - startTime) / 1000)
        }
    }

    private class TestResultAvailable(val testResult: TestResult) : RuntimeException()
}
