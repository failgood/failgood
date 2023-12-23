package failgood.internal

import failgood.*
import failgood.dsl.*
import failgood.dsl.ContextDSL
import failgood.dsl.ResourcesDSL
import failgood.internal.given.GivenDSLHandler
import failgood.internal.given.RootGivenDSLHandler

/**
 * Executes a single test with all its parent contexts. Called by [TestCollectionExecutor] to execute all
 * tests that it does not have to execute itself
 */
internal class SingleTestExecutor<RootGiven, TestGivenType>(
    private val test: ContextPath,
    val testContextHandler: ClonableTestContext<TestGivenType>,
    val resourcesCloser: ResourcesCloser,
    private val rootContextFunction: ContextFunctionWithGiven<RootGiven>,
    val givenFunction: suspend () -> RootGiven
) {
    private val startTime = System.nanoTime()

    suspend fun execute(): TestResult {
        val dsl: ContextDSL<RootGiven> =
            ContextFinder(test.container.path.drop(1), RootGivenDSLHandler(givenFunction))
        return try {
            dsl.(rootContextFunction)()
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

    private inner class ContextFinder<GivenType>(
        private val contexts: List<String>,
        val givenDSLHandler: GivenDSLHandler<GivenType>
    ) :
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
            given: GivenFunction<GivenType, ContextDependency>,
            contextFunction: suspend ContextDSL<ContextDependency>.() -> Unit
        ) {
            if (findTest || contexts.first() != name) return

            ContextFinder(contexts.drop(1), givenDSLHandler.add(given)).contextFunction()
        }

        override suspend fun it(
            name: String,
            tags: Set<String>,
            ignored: Ignored?,
            function: TestFunction<GivenType>
        ) {
            if (findTest && test.name == name) {
                @Suppress("UNCHECKED_CAST")
                throw TestResultAvailable(
                    executeTest(
                        function as TestFunction<TestGivenType>,
                        givenDSLHandler as GivenDSLHandler<TestGivenType>
                    )
                )
            }
        }

        private suspend fun executeTest(
            function: TestFunction<TestGivenType>,
            givenDSLHandler: GivenDSLHandler<TestGivenType>
        ): TestResult {
            try {
                val given = givenDSLHandler.given()
                val testDSLWithGiven = testContextHandler.withGiven(given)
                (testDSLWithGiven as TestDSLWithGiven<TestGivenType>).function()
            } catch (e: Throwable) {
                val failure = Failure(e)
                try {
                    resourcesCloser.callAfterEach(testContextHandler, failure)
                } catch (_: Throwable) {}
                try {
                    resourcesCloser.closeAutoCloseables()
                } catch (_: Throwable) {}
                return failure
            }
            val success =
                try {
                    val success = Success((System.nanoTime() - startTime) / 1000)
                    resourcesCloser.callAfterEach(testContextHandler, success)
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
