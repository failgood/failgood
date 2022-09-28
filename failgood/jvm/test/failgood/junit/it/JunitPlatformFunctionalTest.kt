@file:OptIn(ExperimentalTime::class)

package failgood.junit.it

import failgood.Ignored
import failgood.Test
import failgood.describe
import failgood.junit.it.fixtures.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherFactory
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import kotlin.reflect.KClass
import kotlin.test.assertNotNull
import kotlin.time.ExperimentalTime

@Test
class JunitPlatformFunctionalTest {
    data class Results(
        val rootResult: TestExecutionResult,
        val results: MutableMap<TestIdentifier, TestExecutionResult>
    )

    val context = describe("The Junit Platform Engine") {
        it("can execute test in a class") {
            assertSuccess(executeSingleTest(DuplicateTestNameTest::class))
        }
        describe("duplicate test names") {
            it("are correctly handled in root contexts") {
                assertSuccess(executeSingleTest(DoubleTestNamesInRootContextTestFixture::class))
            }
            it("are correctly handled in sub contexts") {
                assertSuccess(executeSingleTest(DoubleTestNamesInSubContextTestFixture::class))
            }
            it("works even in deeply nested contexts") {
                assertSuccess(executeSingleTest(DeeplyNestedDuplicateTestFixture::class))
            }
        }
        it("works for a failing context or root context") {
            val selectors = listOf(
                DuplicateRootWithOneTestFixture::class,
                DuplicateTestNameTest::class,
                FailingContext::class,
                FailingRootContext::class,
                IgnoredTestFixture::class,
                TestFixture::class,
                TestWithNestedContextsFixture::class
            ).map { selectClass(it.qualifiedName) }
            val r = execute(selectors)
            expectThat(r.rootResult) {
                get { status }.isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
            }
            expectThat(r.results).hasSize(24)
            expectThat(
                r.results.entries.filter { it.value.status == TestExecutionResult.Status.FAILED }
                    .map { it.key.displayName }
            ).containsExactlyInAnyOrder("Failing Root Context", "error in context")
        }
        it(
            "works with Blockhound installed",
            ignored = Ignored.Because("this needs more work and I stopped using blockhound")
        ) {
            val result = executeSingleTest(BlockhoundTestFixture::class)
            assertSuccess(result)
            val entries = result.results.entries

            assert(entries.size > 1)
            val throwable = assertNotNull(
                entries.singleOrNull { (key, _) ->
                    key.displayName == "interop with blockhound"
                }?.value?.throwable
            )
            assert(throwable.get().message?.contains("blocking") == true)
        }
        it("returns tests in the order that they are declared in the file") {
            val testPlan = LauncherFactory.create().discover(
                launcherDiscoveryRequest(
                    listOf(selectClass(TestOrderFixture::class.qualifiedName))
                )
            )
            val root: TestIdentifier = assertNotNull(testPlan.roots.singleOrNull())
            val rootContext = assertNotNull(testPlan.getChildren(root).singleOrNull())
            val (tests, subcontexts) = testPlan.getChildren(rootContext).partition { it.isTest }
            assert(tests.map { it.displayName } == listOf("test 1", "test 2", "test 3", "test 4"))
            subcontexts.forEach {
                assert(
                    testPlan.getChildren(it).map { it.displayName } == listOf(
                        "test 1", "test 2", "test 3", "test 4"
                    )
                )
            }
        }
        it("returns uniqueIds that it understands (uniqueid round-trip test)") {
            // run a test by className
            val result = executeSingleTest(TestFixture::class)
            expectThat(result.rootResult).get { status }.isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
            val testName = TestFixture.testName
            val descriptor: TestIdentifier = assertNotNull(
                result.results.keys.singleOrNull { it.displayName == testName }
            )
            val uniqueId = descriptor.uniqueId
            // now use the uniqueid that we just returned to run the same test again
            val newResult = execute(listOf(selectUniqueId(uniqueId)))
            val tests = newResult.results.keys.filter { it.isTest }.map {
                it.displayName
            }
            assert(tests.singleOrNull() == testName)
        }
        describe("error handling") {
            it("correctly reports exceptions in afterEach as test failures") {
                val result = executeSingleTest(TestFixtureWithFailingTestAndAfterEach::class)
                assertSuccess(result)
                val testResult =
                    assertNotNull(result.results.entries.singleOrNull { it.key.displayName == "the test name" }).value
                assert(testResult.throwable.get().message == "fail")
            }
            it("correctly handles test that fail in their second pass") {
                assertSuccess(executeSingleTest(TestFixtureThatFailsAfterFirstPass::class))
            }
        }
    }

    private suspend fun executeSingleTest(singleTest: KClass<*>): Results =
        execute(listOf(selectClass(singleTest.qualifiedName)))

    private suspend fun execute(discoverySelectors: List<DiscoverySelector>): Results {
        val listener = TEListener()
        LauncherFactory.create().execute(launcherDiscoveryRequest(discoverySelectors), listener)
        // await with timeout to make sure the test does not hang
        val rootResult = withTimeout(5000) { listener.rootResult.await() }
        return Results(rootResult, listener.results)
    }

    private fun assertSuccess(result: Results) {
        assert(result.rootResult.status == TestExecutionResult.Status.SUCCESSFUL) {
            result.rootResult.throwable.get().stackTraceToString()
        }
    }

    class TEListener : TestExecutionListener {
        val rootResult = CompletableDeferred<TestExecutionResult>()
        val results = mutableMapOf<TestIdentifier, TestExecutionResult>()
        override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
            results[testIdentifier] = testExecutionResult
            val parentId = testIdentifier.parentId
            if (!parentId.isPresent) {
                rootResult.complete(testExecutionResult)
            }
        }
    }
}
