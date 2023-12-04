package failgood.internal

import failgood.*
import failgood.dsl.*
import failgood.dsl.ContextDSL
import failgood.dsl.ResourcesDSL

/**
 * Executes a single test with all its parent contexts. Called by ContextExecutor to execute all
 * tests that it does not have to execute itself
 */
internal class SingleTestExecutor<GivenType>(
    private val test: ContextPath,
    val testDSL: TestDSLWithGiven<GivenType>,
    val resourcesCloser: ResourcesCloser,
    private val rootContextLambda: ContextLambda
) {
    private val startTime = System.nanoTime()

    suspend fun execute(): TestResult {
        val dsl: ContextDSL<Unit> = ContextFinder(test.container.path.drop(1))
        return try {
            dsl.(rootContextLambda)()
            throw FailGoodException(
                "test not found: $test.\n" +
                    "please make sure your test names contain no random parts"
            )
        } catch (e: TestResultAvailable) {
            e.testResult
        } catch (e: Throwable) {
            Failure(e)
        }
    }

    private inner class ContextFinder<GivenType>(private val contexts: List<String>) :
        ContextDSL<GivenType>,
        ResourcesDSL by resourcesCloser,
        ContextOnlyResourceDSL by resourcesCloser {
        // are we already in the correct context and just waiting for the test?
        val findTest = contexts.isEmpty()

        override fun afterSuite(function: suspend () -> Unit) {}

        override suspend fun <ContextDependency> describe(
            name: String,
            tags: Set<String>,
            isolation: Boolean?,
            ignored: Ignored?,
            given: GivenLambda<GivenType, ContextDependency>,
            contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
        ) {
            if (findTest || contexts.first() != name) return

            ContextFinder<ContextDependency>(contexts.drop(1)).contextLambda()
        }

        override suspend fun it(
            name: String,
            tags: Set<String>,
            ignored: Ignored?,
            function: TestLambda<GivenType>
        ) {
            if (findTest && test.name == name) {
                throw TestResultAvailable(executeTest(function))
            }
        }

        private suspend fun executeTest(function: TestLambda<GivenType>): TestResult {
            try {
                @Suppress("UNCHECKED_CAST") (testDSL as TestDSLWithGiven<GivenType>).function()
            } catch (e: Throwable) {
                val failure = Failure(e)
                try {
                    resourcesCloser.callAfterEach(testDSL, failure)
                } catch (_: Throwable) {}
                try {
                    resourcesCloser.closeAutoCloseables()
                } catch (_: Throwable) {}
                return failure
            }
            val success =
                try {
                    val success = Success((System.nanoTime() - startTime) / 1000)
                    resourcesCloser.callAfterEach(testDSL, success)
                    success
                } catch (e: Throwable) {
                    Failure(e)
                }
            resourcesCloser.closeAutoCloseables()
            return success
        }
    }

    private class TestResultAvailable(val testResult: TestResult) : DSLGotoException()
}