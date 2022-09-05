@file:OptIn(ExperimentalTime::class)

package failgood.junit.it

import failgood.Test
import failgood.describe
import failgood.junit.it.fixtures.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@Test
class JunitPlatformFunctionalTest {
    @OptIn(ExperimentalTime::class)
    @Suppress("unused")
    val context = describe("The Junit Platform Engine") {
        val listener = TEListener()
        it("can execute test in a class") {
            executeSingleTest(DuplicateTestNameTest::class, listener)
            expectThat(listener.rootResult.await()).get { status }.isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
        }
        describe("duplicate test names") {
            it("are correctly handled in root contexts") {
                LauncherFactory.create().execute(
                    launcherDiscoveryRequest(
                        listOf(selectClass(DoubleTestNamesInRootContextTestFixture::class.qualifiedName))
                    ),
                    listener
                )
                val rootResult = listener.rootResult.await()
                assert(rootResult.status == TestExecutionResult.Status.SUCCESSFUL) {
                    rootResult.throwable.get().stackTraceToString()
                }
            }
            it("are correctly handled in sub contexts") {
                LauncherFactory.create().execute(
                    launcherDiscoveryRequest(
                        listOf(selectClass(DoubleTestNamesInSubContextTestFixture::class.qualifiedName))
                    ),
                    listener
                )
                val rootResult = listener.rootResult.await()
                assert(rootResult.status == TestExecutionResult.Status.SUCCESSFUL) {
                    rootResult.throwable.get().stackTraceToString()
                }
            }
            it("works even in deeply nested contexts") {
                LauncherFactory.create().execute(
                    launcherDiscoveryRequest(
                        listOf(selectClass(DeeplyNestedDuplicateTestFixture::class.qualifiedName))
                    ),
                    listener
                )
                val rootResult = listener.rootResult.await()
                assert(rootResult.status == TestExecutionResult.Status.SUCCESSFUL) {
                    rootResult.throwable.get().stackTraceToString()
                }
            }
        }
        it("works for a failing context or root context") {
            val selectors = listOf(
                DuplicateRootWithOneTestFixture::class,
                DuplicateTestNameTest::class,
                FailingContext::class,
                FailingRootContext::class,
                PendingTestFixture::class,
                TestFixture::class,
                TestWithNestedContextsFixture::class
            ).map { selectClass(it.qualifiedName) }
            LauncherFactory.create().execute(
                launcherDiscoveryRequest(selectors), listener
            )

            val result = withTimeout(5.seconds) { listener.rootResult.await() }
            expectThat(result) {
                get { status }.isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
            }
            expectThat(listener.results).hasSize(24)
            expectThat(
                listener.results.entries.filter { it.value.status == TestExecutionResult.Status.FAILED }
                    .map { it.key.displayName }
            ).containsExactlyInAnyOrder("Failing Root Context", "error in context")
        }
        ignore("works with Blockhound installed") {
            LauncherFactory.create().execute(
                launcherDiscoveryRequest(
                    listOf(selectClass(BlockhoundTestFixture::class.qualifiedName))
                ),
                listener
            )
            val rootResult = listener.rootResult.await()
            assert(rootResult.status == TestExecutionResult.Status.SUCCESSFUL) {
                rootResult.throwable.get().stackTraceToString()
            }
            val entries = listener.results.entries

            assert(entries.size > 1)
            val throwable =
                assertNotNull(
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
            executeSingleTest(TestFixture::class, listener)
            expectThat(listener.rootResult.await()).get { status }.isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
            val testName = TestFixture.testName
            val descriptor: TestIdentifier = assertNotNull(
                listener.results.keys.singleOrNull {
                    it.displayName == testName
                }
            )
            val uniqueId = descriptor.uniqueId
            // now use the uniqueid that we just returned to run the same test again
            val newListener = TEListener()
            LauncherFactory.create().execute(launcherDiscoveryRequest(listOf(selectUniqueId(uniqueId))), newListener)
            expectThat(newListener.rootResult.await()).get { status }.isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
            val tests = newListener.results.keys.filter { it.isTest }.map {
                it.displayName
            }
            assert(tests.singleOrNull() == testName)
        }
        describe("error handling") {
            it("correctly reports exceptions in afterEach as test failures") {
                LauncherFactory.create().execute(
                    launcherDiscoveryRequest(
                        listOf(selectClass(TestFixtureWithFailingTestAndAfterEach::class.qualifiedName))
                    ),
                    listener
                )
                val rootResult = listener.rootResult.await()
                assert(rootResult.status == TestExecutionResult.Status.SUCCESSFUL) {
                    rootResult.throwable.get().stackTraceToString()
                }
            }
        }
    }

    private fun executeSingleTest(singleTest: KClass<*>, listener: TEListener) {
        LauncherFactory.create().execute(
            launcherDiscoveryRequest(
                listOf(selectClass(singleTest.qualifiedName))
            ),
            listener
        )
    }
}

class TEListener : TestExecutionListener {
    val rootResult = CompletableDeferred<TestExecutionResult>()
    val results = mutableMapOf<TestIdentifier, TestExecutionResult>()
    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        results[testIdentifier] = testExecutionResult
        val parentId = testIdentifier.parentId
        if (!parentId.isPresent) rootResult.complete(testExecutionResult)
    }
}
