package failgood.junit.it

import failgood.Ignored
import failgood.Test
import failgood.assert.containsExactlyInAnyOrder
import failgood.dsl.ContextDSL
import failgood.junit.FailGoodJunitTestEngine
import failgood.junit.it.JunitPlatformFunctionalTest.TEListener.Event.Type.FINISHED
import failgood.junit.it.JunitPlatformFunctionalTest.TEListener.Event.Type.REGISTERED
import failgood.junit.it.JunitPlatformFunctionalTest.TEListener.Event.Type.SKIPPED
import failgood.junit.it.JunitPlatformFunctionalTest.TEListener.Event.Type.STARTED
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
import failgood.junit.it.fixtures.SimpleTestFixtureWithMultipleTests
import failgood.junit.it.fixtures.TestFixtureThatFailsAfterFirstPass
import failgood.junit.it.fixtures.TestFixtureWithFailingTestAndAfterEach
import failgood.junit.it.fixtures.TestFixtureWithNonStandardDescribe
import failgood.junit.it.fixtures.TestOrderFixture
import failgood.junit.it.fixtures.TestWithNestedContextsFixture
import failgood.softly.softly
import failgood.testsAbout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherFactory

@Test
object JunitPlatformFunctionalTest {
    data class Results(
        val rootResult: TestExecutionResult,
        val results: Map<TestIdentifier, TestExecutionResult>,
        val testEvents: List<TEListener.Event>
    )

    val tests =
        listOf(
            testsAbout("The Junit Platform Engine") { tests(false) },
            testsAbout("The New Junit Platform Engine") { tests(true) }
        )

    private suspend fun ContextDSL<Unit>.tests(newEngine: Boolean) {
        suspend fun execute(discoverySelectors: List<DiscoverySelector>): Results {
            val listener = TEListener()

            LauncherFactory.create()
                .execute(launcherDiscoveryRequest(discoverySelectors, newEngine), listener)

            // await with timeout to make sure the test does not hang
            val rootResult =
                try {
                    withTimeout(5000) { listener.rootResult.await() }
                } catch (e: TimeoutCancellationException) {
                    throw AssertionError(
                        "Test execution timed out. received results:${listener.results}"
                    )
                }
            return Results(rootResult, listener.results, listener.testEvents)
        }

        suspend fun executeSingleTest(singleTest: KClass<*>): Results =
            execute(listOf(selectClass(singleTest.qualifiedName)))

        describe("ignored contexts", isolation = false) {
            val result = executeSingleTest(IgnoredContextFixture::class)
            it("can execute tests with ignored contexts") {
                assertSuccess(result)
                assert(getFailedTests(result).isEmpty()) {
                    getFailedTests(result).joinToString("\n") {
                        it.value.throwable.get().stackTraceToString()
                    }
                }
            }
            if (newEngine)
                it("correctly reports events for ignored contexts") {
                    assertSuccess(result)
                    assert(getFailedTests(result).isEmpty())
                    val className = IgnoredContextFixture::class.simpleName
                    assertEquals(
                        listOf(
                            Pair(STARTED, "failgood-new"),
                            Pair(REGISTERED, "$className: root context"),
                            Pair(REGISTERED, "ignored context"),
                            Pair(
                                REGISTERED,
                                "context ignored because we are testing subcontext ignoring"
                            ),
                            Pair(STARTED, "$className: root context"),
                            Pair(STARTED, "ignored context"),
                            Pair(
                                SKIPPED,
                                "context ignored because we are testing subcontext ignoring"
                            ),
                            Pair(FINISHED, "ignored context"),
                            Pair(FINISHED, "$className: root context"),
                            Pair(FINISHED, "failgood-new")
                        ),
                        result.testEvents.map { Pair(it.type, it.test.displayName) }
                    )
                }
        }
        describe("ignored tests", isolation = false) {
            val result = executeSingleTest(IgnoredTestFixture::class)
            it("can execute ignored tests") {
                assertSuccess(result)
                assert(getFailedTests(result).isEmpty())
            }
            if (newEngine)
                it("correctly reports events for ignored tests") {
                    assertSuccess(result)
                    assert(getFailedTests(result).isEmpty())
                    assertEquals(
                        listOf(
                            Pair(STARTED, "failgood-new"),
                            Pair(
                                REGISTERED,
                                "${IgnoredTestFixture::class.simpleName}: root context"
                            ),
                            Pair(REGISTERED, "pending test"),
                            Pair(STARTED, "${IgnoredTestFixture::class.simpleName}: root context"),
                            Pair(SKIPPED, "pending test"),
                            Pair(FINISHED, "${IgnoredTestFixture::class.simpleName}: root context"),
                            Pair(FINISHED, "failgood-new")
                        ),
                        result.testEvents.map { Pair(it.type, it.test.displayName) }
                    )
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
                val selectors = listOf(FailingContext::class).map { selectClass(it.qualifiedName) }
                val r = execute(selectors)
                assertSuccess(r)
                assert(
                    getFailedTests(r)
                        .map { it.key.displayName }
                        .containsExactlyInAnyOrder("error in context")
                )
            }
            it("reports failing root contexts") {
                val selectors =
                    listOf(FailingRootContext::class).map { selectClass(it.qualifiedName) }
                val r = execute(selectors)
                assertSuccess(r)
                val failedTests = getFailedTests(r)
                assert(
                    failedTests
                        .map { it.key.displayName }
                        .containsExactlyInAnyOrder(
                            "${FailingRootContext::class.simpleName}: Failing Root Context"
                        )
                )
            }
        }
        // todo: remove this test when we are sure that it does not test anything useful by mistake
        it("works for a failing context or root context") {
            val selectors =
                listOf(
                        DuplicateRootWithOneTestFixture::class,
                        DuplicateTestNameTest::class,
                        FailingContext::class,
                        FailingRootContext::class,
                        IgnoredTestFixture::class,
                        SimpleTestFixture::class,
                        TestWithNestedContextsFixture::class
                    )
                    .map { selectClass(it.qualifiedName) }
            val r = execute(selectors)
            assertSuccess(r)
            softly {
                // just assert that a lot of tests were running. this test is a bit unfocused
                assert(r.results.size > 20) {
                    r.results.entries
                        .sortedBy { it.key.uniqueId }
                        .joinToString("\n") { it.key.uniqueId }
                }
                val failedTests = getFailedTests(r)
                assert(
                    failedTests
                        .map { it.key.displayName }
                        .containsExactlyInAnyOrder(
                            "FailingRootContext: Failing Root Context",
                            "error in context"
                        )
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
            val throwable =
                assertNotNull(
                    entries
                        .singleOrNull { (key, _) -> key.displayName == "interop with blockhound" }
                        ?.value
                        ?.throwable
                )
            assert(throwable.get().message?.contains("blocking") == true)
        }
        it(
            "returns tests in the order that they are declared in the file",
            ignored =
                if (newEngine) Ignored.Because("it does not work with the new engine") else null
        ) {
            val testPlan =
                // force old junit engine even if we are running with the new engine
                LauncherFactory.create(
                        LauncherConfig.builder()
                            .enableTestEngineAutoRegistration(false)
                            .addTestEngines(FailGoodJunitTestEngine())
                            .build()
                    )
                    .discover(
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
                    testPlan.getChildren(testIdentifier).map { it.displayName } ==
                        listOf("test 1", "test 2", "test 3", "test 4")
                )
            }
        }
        describe("running by unique id") {
            it("returns correct uniqueid for non standard describes") {
                val result = executeSingleTest(TestFixtureWithNonStandardDescribe::class)
                assertSuccess(result)

                val testName = "a test in the subcontext"
                val descriptor: TestIdentifier =
                    assertNotNull(result.results.keys.singleOrNull { it.displayName == testName })
                val uniqueId = descriptor.uniqueId
                assert(
                    uniqueId
                        .toString()
                        .contains(TestFixtureWithNonStandardDescribe::class.simpleName!!)
                ) {
                    "our unique ids must contain the class name"
                }
            }
            it("returns uniqueIds that it understands (uniqueid round-trip test)") {
                // run a test by className
                val result = executeSingleTest(SimpleTestFixtureWithMultipleTests::class)
                assertSuccess(result)

                val testName = "a test in the subcontext"
                val descriptor: TestIdentifier =
                    assertNotNull(result.results.keys.singleOrNull { it.displayName == testName })
                val uniqueId = descriptor.uniqueId
                assert(
                    uniqueId
                        .toString()
                        .contains(SimpleTestFixtureWithMultipleTests::class.simpleName!!)
                ) {
                    "our unique ids must contain the class name"
                }
                // now use the uniqueid that we just returned to run the same test again
                val newResult = execute(listOf(selectUniqueId(uniqueId)))
                val tests = newResult.results.keys.filter { it.isTest }.map { it.displayName }
                assert(tests.singleOrNull() == testName)
            }
        }
        describe("error handling") {
            it("correctly reports exceptions in afterEach as test failures") {
                val result = executeSingleTest(TestFixtureWithFailingTestAndAfterEach::class)
                assertSuccess(result)
                val testResult =
                    assertNotNull(
                            result.results.entries.singleOrNull {
                                it.key.displayName == "the test name"
                            }
                        )
                        .value
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
        data class Event(val type: Type, val test: TestIdentifier) {
            enum class Type {
                STARTED,
                FINISHED,
                REGISTERED,
                SKIPPED
            }
        }

        val testEvents = CopyOnWriteArrayList<Event>()
        val rootResult = CompletableDeferred<TestExecutionResult>()
        val results = ConcurrentHashMap<TestIdentifier, TestExecutionResult>()

        override fun dynamicTestRegistered(testIdentifier: TestIdentifier) {
            super.dynamicTestRegistered(testIdentifier)
            testEvents.add(Event(REGISTERED, testIdentifier))
        }

        override fun executionStarted(testIdentifier: TestIdentifier) {
            super.executionStarted(testIdentifier)
            testEvents.add(Event(STARTED, testIdentifier))
        }

        override fun executionSkipped(testIdentifier: TestIdentifier, reason: String?) {
            super.executionSkipped(testIdentifier, reason)
            testEvents.add(Event(SKIPPED, testIdentifier))
        }

        override fun executionFinished(
            testIdentifier: TestIdentifier,
            testExecutionResult: TestExecutionResult
        ) {
            testEvents.add(Event(FINISHED, testIdentifier))
            results[testIdentifier] = testExecutionResult
            val parentId = testIdentifier.parentId
            if (!parentId.isPresent) {
                rootResult.complete(testExecutionResult)
            }
        }
    }
}
