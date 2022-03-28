package failgood.junit.it

import failgood.Test
import failgood.describe
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_TEST_CLASS_SUFFIX
import failgood.junit.it.fixtures.BlockhoundTestFixture
import failgood.junit.it.fixtures.DeeplyNestedDuplicateTestFixture
import failgood.junit.it.fixtures.DoubleTestNamesInRootContextTestFixture
import failgood.junit.it.fixtures.DoubleTestNamesInSubContextTestFixture
import failgood.junit.it.fixtures.DuplicateRootWithOneTestFixture
import failgood.junit.it.fixtures.DuplicateTestNameTest
import failgood.junit.it.fixtures.FailingContext
import failgood.junit.it.fixtures.FailingRootContext
import failgood.junit.it.fixtures.PendingTestFixture
import failgood.junit.it.fixtures.TestFixture
import failgood.junit.it.fixtures.TestOrderFixture
import failgood.junit.it.fixtures.TestWithNestedContextsFixture
import kotlinx.coroutines.CompletableDeferred
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

@Test
class JunitPlatformFunctionalTest {
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
                        listOf(selectClass(DoubleTestNamesInRootContextTestFixture::class.qualifiedName)),
                        mapOf(CONFIG_KEY_TEST_CLASS_SUFFIX to "")
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
                        listOf(selectClass(DoubleTestNamesInSubContextTestFixture::class.qualifiedName)),
                        mapOf(CONFIG_KEY_TEST_CLASS_SUFFIX to "")
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
                        listOf(selectClass(DeeplyNestedDuplicateTestFixture::class.qualifiedName)),
                        mapOf(CONFIG_KEY_TEST_CLASS_SUFFIX to "")
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
                launcherDiscoveryRequest(selectors, mapOf(CONFIG_KEY_TEST_CLASS_SUFFIX to "")), listener
            )
            val result = listener.rootResult.await()
            expectThat(result) {
                get { status }.isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
            }
            expectThat(listener.results).hasSize(24)
            expectThat(
                listener.results.entries.filter { it.value.status == TestExecutionResult.Status.FAILED }
                    .map { it.key.displayName }
            ).containsExactlyInAnyOrder("Failing Root Context", "error in context")
        }
        pending("works with Blockhound installed") {
            LauncherFactory.create().execute(
                launcherDiscoveryRequest(
                    listOf(selectClass(BlockhoundTestFixture::class.qualifiedName)),
                    mapOf(CONFIG_KEY_TEST_CLASS_SUFFIX to "")
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
                    entries.singleOrNull { (key, _) -> key.displayName == "interop with blockhound" }
                        ?.value?.throwable
                )
            assert(throwable.get().message?.contains("blocking") == true)
        }
        it("returns tests in the order that they are declared in the file") {
            val testPlan = LauncherFactory.create()
                .discover(launcherDiscoveryRequest(listOf(selectClass(TestOrderFixture::class.qualifiedName))))
            val root: TestIdentifier = assertNotNull(testPlan.roots.singleOrNull())
            val rootContext = assertNotNull(testPlan.getChildren(root).singleOrNull())
            val (tests, subcontexts)  = testPlan.getChildren(rootContext).partition { it.isTest }
            assert(tests.map { it.displayName } == listOf("test 1", "test 2", "test 3", "test 4"))
            subcontexts.forEach {
                assert(testPlan.getChildren(it).map { it.displayName } == listOf("test 1", "test 2", "test 3", "test 4"))
            }
        }
        it("returns uniqueIds that it understands (uniqueid roundtrip test)") {
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
            LauncherFactory.create()
                .execute(launcherDiscoveryRequest(listOf(selectUniqueId(uniqueId))), newListener)
            expectThat(newListener.rootResult.await()).get { status }.isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
            val tests = newListener.results.keys.filter { it.isTest }.map {
                it.displayName
            }
            assert(tests.singleOrNull() == testName)
        }
    }

    private fun executeSingleTest(singleTest: KClass<*>, listener: TEListener) {
        LauncherFactory.create()
            .execute(launcherDiscoveryRequest(listOf(selectClass(singleTest.qualifiedName))), listener)
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
