package failgood.junit.it

import failgood.Ignored
import failgood.Test
import failgood.assert.containsExactlyInAnyOrder
import failgood.describe
import failgood.dsl.ContextDSL
import failgood.junit.it.fixtures.BlockhoundTestFixture
import failgood.junit.it.fixtures.DeeplyNestedDuplicateTestFixture
import failgood.junit.it.fixtures.DoubleTestNamesInRootContextTestFixture
import failgood.junit.it.fixtures.DoubleTestNamesInSubContextTestFixture
import failgood.junit.it.fixtures.DuplicateRootWithOneTestFixture
import failgood.junit.it.fixtures.DuplicateTestNameTest
import failgood.junit.it.fixtures.FailingContext
import failgood.junit.it.fixtures.FailingRootContext
import failgood.junit.it.fixtures.IgnoredContextFixture
import failgood.junit.it.fixtures.IgnoredTestFixture
import failgood.junit.it.fixtures.SimpleClassTestFixture
import failgood.junit.it.fixtures.SimpleTestFixture
import failgood.junit.it.fixtures.TestFixtureThatFailsAfterFirstPass
import failgood.junit.it.fixtures.TestFixtureWithFailingTestAndAfterEach
import failgood.junit.it.fixtures.TestOrderFixture
import failgood.junit.it.fixtures.TestWithNestedContextsFixture
import failgood.softly.softly
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass
import kotlin.test.assertNotNull

@Test
object JunitPlatformFunctionalTest {
    data class Results(
        val rootResult: TestExecutionResult,
        val results: Map<TestIdentifier, TestExecutionResult>,
        val testEvents: List<TEListener.Event>
    )

    val context = listOf(
        describe("The Junit Platform Engine") {
            tests(false)
        },
        describe("The New Junit Platform Engine") {
            tests(true)
        }
    )

    private suspend fun ContextDSL<Unit>.tests(newEngine: Boolean) {
        suspend fun execute(discoverySelectors: List<DiscoverySelector>): Results {
            val listener = TEListener()

            LauncherFactory.create().execute(launcherDiscoveryRequest(discoverySelectors, newEngine), listener)

            // await with timeout to make sure the test does not hang
            val rootResult = try {
                withTimeout(5000) { listener.rootResult.await() }
            } catch (e: TimeoutCancellationException) {
                throw AssertionError("Test execution timed out. received results:${listener.results}")
            }
            return Results(rootResult, listener.results, listener.testEvents)
        }

        suspend fun executeSingleTest(singleTest: KClass<*>): Results =
            execute(listOf(selectClass(singleTest.qualifiedName)))

        describe("ignored contexts") {
            it("can execute tests with ignored contexts") {
                val result = executeSingleTest(IgnoredContextFixture::class)
                assertSuccess(result)
                assert(getFailedTests(result).isEmpty()) {
                    getFailedTests(result).joinToString("\n") { it.value.throwable.get().stackTraceToString() }
                }
            }
        }
        describe("ignored tests", isolation = false) {
            val result = executeSingleTest(IgnoredTestFixture::class)
            it("can execute ignored tests") {
                assertSuccess(result)
                assert(getFailedTests(result).isEmpty())
            }
            if (newEngine)
                it("correctly reports context started event for ignored tests") {
                    assertSuccess(result)
                    result.testEvents[0].let {
                        assert(it is TEListener.Event.TestStarted && it.test.uniqueId.toString() == "[engine:failgood]")
                    }
                    result.testEvents[1].let {
                        assert(it is TEListener.Event.TestRegistered && it.test.displayName == "the root context")
                    }
                    result.testEvents[2].let {
                        assert(it is TEListener.Event.TestRegistered && it.test.displayName == "pending test")
                    }
                    result.testEvents[3].let {
                        assert(it is TEListener.Event.TestStarted && it.test.displayName == "the root context")
                    }
                    result.testEvents[4].let {
                        assert(it is TEListener.Event.TestSkipped && it.test.displayName == "pending test")
                    }
                    result.testEvents[5].let {
                        assert(it is TEListener.Event.TestFinished && it.test.displayName == "the root context")
                    }
                    assert(getFailedTests(result).isEmpty())
                }
        }
        it("can execute duplicate root") {
            assertSuccess(executeSingleTest(DuplicateRootWithOneTestFixture::class))
        }
        it("can execute a simple test defined in an object") {
            assertSuccess(executeSingleTest(SimpleTestFixture::class))
        }
        it("can execute a simple test defined in a class") {
            assertSuccess(executeSingleTest(SimpleClassTestFixture::class))
        }
        it("executes contexts that contain tests with the same names") {
            val result = executeSingleTest(DuplicateTestNameTest::class)
            assertSuccess(result)
            assert(getFailedTests(result).isEmpty())
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
        describe("failing contexts") {
            it("reports failing contexts") {
                val selectors = listOf(
                    FailingContext::class
                ).map { selectClass(it.qualifiedName) }
                val r = execute(selectors)
                assertSuccess(r)
                assert(
                    getFailedTests(r)
                        .map { it.key.displayName }.containsExactlyInAnyOrder("error in context")
                )
            }
            it("reports failing root contexts") {
                val selectors = listOf(
                    FailingRootContext::class
                ).map { selectClass(it.qualifiedName) }
                val r = execute(selectors)
                assertSuccess(r)
                val failedTests = getFailedTests(r)
                assert(failedTests.map { it.key.displayName }.containsExactlyInAnyOrder("Failing Root Context"))
            }
        }
        // todo: remove this test when we are sure that it does not test anything useful by mistake
        it("works for a failing context or root context") {
            val selectors = listOf(
                DuplicateRootWithOneTestFixture::class,
                DuplicateTestNameTest::class,
                FailingContext::class,
                FailingRootContext::class,
                IgnoredTestFixture::class,
                SimpleTestFixture::class,
                TestWithNestedContextsFixture::class
            ).map { selectClass(it.qualifiedName) }
            val r = execute(selectors)
            assertSuccess(r)
            softly {
                // just assert that a lot of tests were running. this test is a bit unfocused
                assert(r.results.size > 20) {
                    r.results.entries.sortedBy { it.key.uniqueId }.joinToString("\n") { it.key.uniqueId }
                }
                assert(
                    getFailedTests(r)
                        .map { it.key.displayName }
                        .containsExactlyInAnyOrder("Failing Root Context", "error in context")
                )
            }
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
        it(
            "returns tests in the order that they are declared in the file",
            ignored = Ignored.Because("it does not work with the new engine")
        ) {
            val testPlan = LauncherFactory.create().discover(
                launcherDiscoveryRequest(
                    listOf(selectClass(TestOrderFixture::class.qualifiedName))
                )
            )
            val root: TestIdentifier = assertNotNull(testPlan.roots.singleOrNull())
            val rootContext = assertNotNull(testPlan.getChildren(root).singleOrNull())
            val (tests, subcontexts) = testPlan.getChildren(rootContext).partition { it.isTest }
            assert(tests.map { it.displayName } == listOf("test 1", "test 2", "test 3", "test 4"))
            subcontexts.forEach { testIdentifier ->
                assert(
                    testPlan.getChildren(testIdentifier).map { it.displayName } == listOf(
                        "test 1", "test 2", "test 3", "test 4"
                    )
                )
            }
        }

        it("returns uniqueIds that it understands (uniqueid round-trip test)") {
            // run a test by className
            val result = executeSingleTest(SimpleTestFixture::class)
            assertSuccess(result)
            val testName = SimpleTestFixture.TEST_NAME
            val descriptor: TestIdentifier =
                assertNotNull(result.results.keys.singleOrNull { it.displayName == testName })
            val uniqueId = descriptor.uniqueId
            assert(
                uniqueId.toString().contains(SimpleTestFixture::class.simpleName!!)
            ) { "our unique ids contain the class name" }
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

    private fun getFailedTests(r: Results) =
        r.results.entries.filter { it.value.status == TestExecutionResult.Status.FAILED }

    private fun assertSuccess(result: Results) {
        assert(result.rootResult.status == TestExecutionResult.Status.SUCCESSFUL) {
            result.rootResult.throwable.get().stackTraceToString()
        }
    }

    class TEListener : TestExecutionListener {
        sealed interface Event {
            val test: TestIdentifier

            data class TestStarted(override val test: TestIdentifier) : Event
            data class TestFinished(override val test: TestIdentifier) : Event
            data class TestRegistered(override val test: TestIdentifier) : Event
            data class TestSkipped(override val test: TestIdentifier) : Event
        }

        val testEvents = CopyOnWriteArrayList<Event>()
        val rootResult = CompletableDeferred<TestExecutionResult>()
        val results = ConcurrentHashMap<TestIdentifier, TestExecutionResult>()
        override fun dynamicTestRegistered(testIdentifier: TestIdentifier) {
            super.dynamicTestRegistered(testIdentifier)
            testEvents.add(Event.TestRegistered(testIdentifier))
        }

        override fun executionStarted(testIdentifier: TestIdentifier) {
            super.executionStarted(testIdentifier)
            testEvents.add(Event.TestStarted(testIdentifier))
        }

        override fun executionSkipped(testIdentifier: TestIdentifier, reason: String?) {
            super.executionSkipped(testIdentifier, reason)
            testEvents.add(Event.TestSkipped(testIdentifier))
        }

        override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
            testEvents.add(Event.TestFinished(testIdentifier))
            results[testIdentifier] = testExecutionResult
            val parentId = testIdentifier.parentId
            if (!parentId.isPresent) {
                rootResult.complete(testExecutionResult)
            }
        }
    }
}
