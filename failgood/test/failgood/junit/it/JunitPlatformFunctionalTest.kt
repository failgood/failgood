package failgood.junit.it

import failgood.Ignored
import failgood.Test
import failgood.assert.containsExactlyInAnyOrder
import failgood.dsl.ContextDSL
import failgood.junit.FailGoodJunitTestEngineConstants
import failgood.junit.JunitEngine
import failgood.junit.it.JunitPlatformFunctionalTest.TestTestExecutionListener.Event.Type.FINISHED
import failgood.junit.it.JunitPlatformFunctionalTest.TestTestExecutionListener.Event.Type.REGISTERED
import failgood.junit.it.JunitPlatformFunctionalTest.TestTestExecutionListener.Event.Type.SKIPPED
import failgood.junit.it.JunitPlatformFunctionalTest.TestTestExecutionListener.Event.Type.STARTED
import failgood.junit.it.fixtures.*
import failgood.junit.legacy.LegacyJUnitTestEngine
import failgood.softly.softly
import failgood.testsAbout
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Test
object JunitPlatformFunctionalTest {
    data class Results(
        val rootResult: TestExecutionResult,
        val results: Map<TestIdentifier, TestExecutionResult>,
        val testEvents: List<TestTestExecutionListener.Event>
    )

    val tests =
        listOf(
            testsAbout("The Junit Platform Engine") { tests(false) },
            testsAbout("The New Junit Platform Engine") { tests(true) }
        )

    private suspend fun ContextDSL<Unit>.tests(newEngine: Boolean) {
        suspend fun execute(discoverySelectors: List<DiscoverySelector>): Results {
            val listener = TestTestExecutionListener(newEngine)


            LauncherFactory.create(
                LauncherConfig.builder()
                    .enableTestEngineAutoRegistration(false)
                    .addTestEngines(if (newEngine) JunitEngine() else LegacyJUnitTestEngine()).build()
            )
                .execute(launcherDiscoveryRequest(discoverySelectors), listener)

            // await with timeout to make sure the test does not hang
            val rootResult =
                try {
                    withTimeout(5000) { listener.rootResult.await() }
                } catch (e: TimeoutCancellationException) {
                    throw AssertionError(
                        "Test execution timed out. received results:${listener.results}"
                    )
                }
            assert(listener.errors.isEmpty()) {
                "errors: ${listener.errors}\nevents: ${
                    listener.testEvents.joinToString("\n") {
                        "${it.type}:${it.test.uniqueId}"
                    }
                }"
            }
            return Results(rootResult, listener.results, listener.testEvents)
        }

        suspend fun executeSingleTest(singleTest: KClass<*>): Results =
            execute(listOf(selectClass(singleTest.qualifiedName)))

        describe("ignored contexts", isolation = false) {
            val result = executeSingleTest(IgnoredContextFixture::class)
            it("can execute tests with ignored contexts") {
                assertTestExecutionSucceeded(result)
                assert(getFailedTests(result).isEmpty()) {
                    getFailedTests(result).joinToString("\n") {
                        it.value.throwable.get().stackTraceToString()
                    }
                }
            }
            if (newEngine)
                it("correctly reports events for ignored contexts") {
                    assertTestExecutionSucceeded(result)
                    assert(getFailedTests(result).isEmpty())
                    val className = IgnoredContextFixture::class.simpleName
                    assertEquals(
                        listOf(
                            Pair(STARTED, FailGoodJunitTestEngineConstants.ID),
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
                            Pair(FINISHED, FailGoodJunitTestEngineConstants.ID)
                        ),
                        result.testEvents.map { Pair(it.type, it.test.displayName) }
                    )
                }
        }
        describe("ignored tests", isolation = false) {
            val result = executeSingleTest(IgnoredTestFixture::class)
            it("can execute ignored tests") {
                assertTestExecutionSucceeded(result)
                assert(getFailedTests(result).isEmpty())
            }
            if (newEngine)
                it("correctly reports events for ignored tests") {
                    assertTestExecutionSucceeded(result)
                    assert(getFailedTests(result).isEmpty())
                    assertEquals(
                        listOf(
                            Pair(STARTED, FailGoodJunitTestEngineConstants.ID),
                            Pair(
                                REGISTERED,
                                "${IgnoredTestFixture::class.simpleName}: root context"
                            ),
                            Pair(REGISTERED, "pending test"),
                            Pair(STARTED, "${IgnoredTestFixture::class.simpleName}: root context"),
                            Pair(SKIPPED, "pending test"),
                            Pair(FINISHED, "${IgnoredTestFixture::class.simpleName}: root context"),
                            Pair(FINISHED, FailGoodJunitTestEngineConstants.ID)
                        ),
                        result.testEvents.map { Pair(it.type, it.test.displayName) }
                    )
                }
        }
        it("can execute duplicate root") {
            val result = executeSingleTest(DuplicateRootWithOneTest::class)
            assertTestExecutionSucceeded(result)
            // engine root and 2 contexts and 2 tests
            assert(result.results.map { it.key.uniqueId }.size == 5)
        }
        it("can execute a simple test defined in an object") {
            assertTestExecutionSucceeded(executeSingleTest(SimpleTestFixture::class))
        }
        it("can execute a simple test defined in a class") {
            assertTestExecutionSucceeded(executeSingleTest(SimpleClassTestFixture::class))
        }
        it("executes contexts that contain tests with the same names") {
            val result = executeSingleTest(DuplicateTestNameTest::class)
            assertTestExecutionSucceeded(result)
            assert(getFailedTests(result).isEmpty())
        }
        describe("duplicate test names") {
            it("are correctly handled in root contexts") {
                assertTestExecutionSucceeded(executeSingleTest(DoubleTestNamesInRootContextTestFixture::class))
            }
            it("are correctly handled in sub contexts") {
                assertTestExecutionSucceeded(executeSingleTest(DoubleTestNamesInSubContextTestFixture::class))
            }
            it("works even in deeply nested contexts") {
                assertTestExecutionSucceeded(executeSingleTest(DeeplyNestedDuplicateTestFixture::class))
            }
        }
        describe("failing contexts") {
            it("reports failing contexts") {
                val r = execute(listOf(selectClass(FailingContext::class.qualifiedName)))
                assertTestExecutionSucceeded(r)
                val (failedTest, _) = assertNotNull(getFailedTests(r).singleOrNull())
                assert(failedTest.displayName == "error in context")
            }
            it("reports failing root contexts") {
                val r = execute(listOf(selectClass(FailingRootContext::class.qualifiedName)))
                assertTestExecutionSucceeded(r)
                val (failedTest, testResult) = assertNotNull(getFailedTests(r).singleOrNull())
                assert(failedTest.displayName == "${FailingRootContext::class.simpleName}: Failing Root Context")
                assert(testResult.throwable.get().message == "root context failed")
            }
        }
        // todo: remove this test when we are sure that it does not test anything useful by mistake
        it("works for a failing context or root context") {
            val selectors =
                listOf(
                    DuplicateRootWithOneTest::class,
                    DuplicateTestNameTest::class,
                    FailingContext::class,
                    FailingRootContext::class,
                    IgnoredTestFixture::class,
                    SimpleTestFixture::class,
                    TestWithNestedContextsFixture::class
                )
                    .map { selectClass(it.qualifiedName) }
            val r = execute(selectors)
            assertTestExecutionSucceeded(r)
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
            assertTestExecutionSucceeded(result)
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
                        .addTestEngines(LegacyJUnitTestEngine())
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
                assertTestExecutionSucceeded(result)

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
            describe("returns uniqueIds that it understands (uniqueid round-trip test)") {
                suspend fun roundTrip(testClass: KClass<*>): String? {
                    val result = executeSingleTest(testClass)
                    assertTestExecutionSucceeded(result)

                    val testName = "a test in the subcontext"
                    val descriptor: TestIdentifier =
                        assertNotNull(result.results.keys.singleOrNull { it.displayName == testName })
                    val uniqueId = descriptor.uniqueId
                    assert(
                        uniqueId
                            .toString()
                            .contains(testClass.simpleName!!)
                    ) {
                        "our unique ids must contain the class name"
                    }
                    // now use the uniqueid that we just returned to run the same test again
                    val newResult = execute(listOf(selectUniqueId(uniqueId)))
                    val tests = newResult.results.keys.filter { it.isTest }.map { it.displayName }
                    assert(tests.singleOrNull() == testName)
                    return uniqueId
                }
                it("works for named test collections") {
                    roundTrip(SimpleTestFixtureWithMultipleTests::class)
                }
                it("works for unnamed test collections") {
                    assertEquals(
                        // document how the uniqueid looks
                        if (newEngine) "[engine:failgood]/[class:SimpleUnnamedTestFixtureWithMultipleTests(failgood.junit.it.fixtures.SimpleUnnamedTestFixtureWithMultipleTests)]/[class:a context in the root context]/[method:a test in the subcontext]"
                        else "[engine:failgood-legacy]/[class:SimpleUnnamedTestFixtureWithMultipleTests(failgood.junit.it.fixtures.SimpleUnnamedTestFixtureWithMultipleTests)]/[class:a context in the root context]/[method:a test in the subcontext]",
                        roundTrip(SimpleUnnamedTestFixtureWithMultipleTests::class)
                    )
                }
            }
        }
        describe("error handling") {
            it("correctly reports exceptions in afterEach as test failures") {
                val result = executeSingleTest(TestFixtureWithFailingTestAndAfterEach::class)
                assertTestExecutionSucceeded(result)
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
                assertTestExecutionSucceeded(executeSingleTest(TestFixtureThatFailsAfterFirstPass::class))
            }
        }
    }

    private fun getFailedTests(r: Results) =
        r.results.entries.filter { it.value.status == TestExecutionResult.Status.FAILED }

    /** Checks that the test execution was a success. It does not check that no test failed */
    private fun assertTestExecutionSucceeded(result: Results) {
        assert(result.rootResult.status == TestExecutionResult.Status.SUCCESSFUL) {
            result.rootResult.throwable.get().stackTraceToString()
        }
    }

    /**
     * this listener will record events and also check that the events order is correct. registed events are only
     * checked for the new engine because in the old engine all tests are returned at discover time and need not be
     * registered
     */
    class TestTestExecutionListener(private val checkRegisterEvent: Boolean) : TestExecutionListener {
        data class Event(val type: Type, val test: TestIdentifier) {
            enum class Type {
                STARTED,
                FINISHED,
                REGISTERED,
                SKIPPED
            }
        }

        val startedTestUniqueIds = ConcurrentHashMap.newKeySet<String>()
        val registeredTestUniqueIds = ConcurrentHashMap.newKeySet<String>()
        val errors = CopyOnWriteArrayList<String>()
        val testEvents = CopyOnWriteArrayList<Event>()
        val rootResult = CompletableDeferred<TestExecutionResult>()
        val results = ConcurrentHashMap<TestIdentifier, TestExecutionResult>()

        override fun dynamicTestRegistered(testIdentifier: TestIdentifier) {
            super.dynamicTestRegistered(testIdentifier)
            if (!registeredTestUniqueIds.add(testIdentifier.uniqueId))
                errors.add(
                    "duplicate test uniqueid registered: ${testIdentifier.uniqueId}. \nregistered uniqueIds: $registeredTestUniqueIds\n"
                )
            testEvents.add(Event(REGISTERED, testIdentifier))
        }

        override fun executionStarted(testIdentifier: TestIdentifier) {
            // the root test identifier is already registered, so we check only elements with parentId
            if (testIdentifier.parentId.isPresent) {
                // check that the test that is starting was registered
                if (checkRegisterEvent && !registeredTestUniqueIds.contains(testIdentifier.uniqueId)) errors.add(
                    "start event received for ${testIdentifier.uniqueId} which was not registered"
                )
                // check that the parent is already started
                testIdentifier.parentId.get()
                    .let { if (!startedTestUniqueIds.contains(it)) errors.add("start event received for ${testIdentifier.uniqueId} whose parent with uniqueid $it was not started") }
            }

            if (!startedTestUniqueIds.add(testIdentifier.uniqueId))
                errors.add("duplicate uniqueid started: ${testIdentifier.uniqueId}")

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
            if (!startedTestUniqueIds.contains(testIdentifier.uniqueId))
                errors.add("finished event received for $testIdentifier which was not started")
            testEvents.add(Event(FINISHED, testIdentifier))
            results[testIdentifier] = testExecutionResult
            val parentId = testIdentifier.parentId
            if (!parentId.isPresent) {
                rootResult.complete(testExecutionResult)
            }
        }
    }
}
