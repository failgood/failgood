package failgood.junit.it

import failgood.Test
import failgood.describe
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_TEST_CLASS_SUFFIX
import failgood.junit.it.fixtures.*
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
        describe("works with duplicate test names") {
            it("in root contexts") {
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
            it("in sub contexts") {
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
        }
        it("works for a failing context or root context") {
            val selectors = listOf(
                DuplicateRootWithOneTest::class,
                DuplicateTestNameTest::class,
                FailingContext::class,
                FailingRootContext::class,
                PendingTestFixtureTest::class,
                TestFixtureTest::class,
                TestWithNestedContextsTest::class
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
            ).containsExactlyInAnyOrder("Failing Root Context", "failing context")
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
        it("returns uniqueIds that it understands") {
            // run a test by className
            executeSingleTest(TestFixtureTest::class, listener)
            expectThat(listener.rootResult.await()).get { status }.isEqualTo(TestExecutionResult.Status.SUCCESSFUL)
            val testName = TestFixtureTest.testName
            val descriptor: TestIdentifier = assertNotNull(listener.results.entries.singleOrNull {
                it.key.displayName == testName
            }?.key)
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
