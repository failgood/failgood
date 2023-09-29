package failgood.internal

import failgood.*
import failgood.dsl.ContextDSL
import failgood.dsl.ContextLambda
import failgood.dsl.ResourcesDSL
import failgood.dsl.TestDSL
import failgood.dsl.TestLambda

/**
 * Executes a single test with all its parent contexts
 * Async Called by ContextExecutor to execute all tests that it does not have to execute itself
 */
internal class SingleTestExecutor(
    private val test: ContextPath,
    val testDSL: TestDSL,
    val resourcesCloser: ResourcesCloser,
    private val rootContextLambda: ContextLambda
) {
    private val startTime = System.nanoTime()

    suspend fun execute(): TestResult {
        val dsl: ContextDSL<Unit> = contextDSL({}, test.container.path.drop(1))
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

    private open inner class Base<GivenType> : ContextDSL<GivenType>, ResourcesDSL by resourcesCloser {
        override suspend fun <ContextDependency> describe(
            name: String,
            tags: Set<String>,
            isolation: Boolean?,
            ignored: Ignored?,
            given: suspend () -> ContextDependency,
            contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
        ) {
        }

        override suspend fun it(name: String, tags: Set<String>, ignored: Ignored?, function: TestLambda<GivenType>) {}

        override fun afterSuite(function: suspend () -> Unit) {}
    }

    private inner class ContextFinder<GivenType>(private val contexts: List<String>) : ContextDSL<GivenType>,
        Base<GivenType>() {
            override suspend fun <ContextDependency> describe(
                name: String,
                tags: Set<String>,
                isolation: Boolean?,
                ignored: Ignored?,
                given: suspend () -> ContextDependency,
                contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
            ) {
                if (contexts.first() != name) return

                contextDSL(given, contexts.drop(1)).contextLambda()
            }
        }

    private fun <ContextDependency> contextDSL(
        given: suspend () -> ContextDependency,
        parentContexts: List<String>
    ): ContextDSL<ContextDependency> =
        if (parentContexts.isEmpty()) TestFinder(given) else ContextFinder(parentContexts)

    private inner class TestFinder<GivenType>(val given: suspend () -> GivenType) : Base<GivenType>() {
        override suspend fun it(name: String, tags: Set<String>, ignored: Ignored?, function: TestLambda<GivenType>) {
            if (test.name == name) {
                throw TestResultAvailable(executeTest(function))
            }
        }

        private suspend fun executeTest(function: TestLambda<GivenType>): TestResult {
            try {
                testDSL.function(given())
            } catch (e: Throwable) {
                val failure = Failure(e)
                try {
                    resourcesCloser.callAfterEach(testDSL, failure)
                } catch (_: Throwable) {
                }
                try {
                    resourcesCloser.closeAutoCloseables()
                } catch (_: Throwable) {
                }
                return failure
            }
            val success = try {
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
