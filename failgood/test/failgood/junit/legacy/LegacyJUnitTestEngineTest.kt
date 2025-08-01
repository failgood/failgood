package failgood.junit.legacy

import failgood.Test
import failgood.junit.FailGoodJunitTestEngineConstants
import failgood.junit.it.fixtures.IgnoredTestFixture
import failgood.junit.it.fixtures.SimpleTestFixture
import failgood.junit.it.fixtures.TestWithNestedContextsFixture
import failgood.junit.it.fixtures.TestWithNestedContextsFixture.Companion.CHILD_CONTEXT_1_NAME
import failgood.junit.it.fixtures.TestWithNestedContextsFixture.Companion.CHILD_CONTEXT_2_NAME
import failgood.junit.it.fixtures.TestWithNestedContextsFixture.Companion.ROOT_CONTEXT_NAME
import failgood.junit.it.fixtures.TestWithNestedContextsFixture.Companion.TEST2_NAME
import failgood.junit.it.fixtures.TestWithNestedContextsFixture.Companion.TEST_NAME
import failgood.junit.it.launcherDiscoveryRequest
import failgood.testCollection
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.ConcurrentLinkedQueue
import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.engine.reporting.OutputDirectoryProvider

/** This tests the old junit engine, so it will probably go away at some point */
@Test
class LegacyJUnitTestEngineTest {
    val tests =
        testCollection(LegacyJUnitTestEngine::class) {
            val engine = LegacyJUnitTestEngine()
            it("parses a filter string") {
                val filters =
                    parseFilterString(
                        "The ContextExecutor > with a valid root context" +
                            " > executing all the tests ✔ returns deferred test results")
                assert(
                    filters ==
                        listOf(
                            "The ContextExecutor",
                            "with a valid root context",
                            "executing all the tests",
                            "returns deferred test results"))
            }

            describe(
                "can discover tests",
                given = {
                    autoClose(
                        // if we only call discover on the engine but not execute,
                        // we have to close the execution context manually
                        engine.discover(
                            launcherDiscoveryRequest(
                                listOf(
                                    DiscoverySelectors.selectClass(
                                        SimpleTestFixture::class.qualifiedName))),
                            UniqueId.forEngine(engine.id))) {
                            (it as FailGoodEngineDescriptor).suiteExecutionContext.close()
                        }
                }) {
                    it("returns a root descriptor") {
                        assert(given.isRoot)
                        assert(given.displayName == "FailGood")
                    }
                    it("returns all root contexts") {
                        val child = given.children.single()
                        assert(child.isContainer)
                        assert(
                            child.displayName ==
                                "${SimpleTestFixture::class.simpleName}: ${SimpleTestFixture.ROOT_CONTEXT_NAME}")
                    }
                }
            describe("test execution") {
                it("starts and stops contexts in the correct order") {
                    val testDescriptor =
                        engine.discover(
                            launcherDiscoveryRequest(
                                listOf(
                                    DiscoverySelectors.selectClass(
                                        TestWithNestedContextsFixture::class.qualifiedName))),
                            UniqueId.forEngine(engine.id))
                    val listener = RememberingExecutionListener()
                    engine.execute(
                        ExecutionRequest.create(
                            testDescriptor,
                            listener,
                            NoConfigurationParameters(),
                            NoOutputDirectories()))
                    val result =
                        listener.list
                            .toList()
                            .replace(
                                // we don't know in what order the tests will run
                                setOf(
                                    "start-$TEST_NAME",
                                    "stop-$TEST_NAME",
                                    "start-$TEST2_NAME",
                                    "stop-$TEST2_NAME"),
                                "some-test-event")
                    assert(
                        result ==
                            listOf(
                                "start-${FailGoodJunitTestEngineConstants.DISPLAY_NAME}",
                                "start-${TestWithNestedContextsFixture::class.simpleName}: $ROOT_CONTEXT_NAME",
                                "start-$CHILD_CONTEXT_1_NAME",
                                "start-$CHILD_CONTEXT_2_NAME",
                                "some-test-event",
                                "some-test-event",
                                "some-test-event",
                                "some-test-event",
                                "stop-$CHILD_CONTEXT_2_NAME",
                                "stop-$CHILD_CONTEXT_1_NAME",
                                "stop-${TestWithNestedContextsFixture::class.simpleName}: $ROOT_CONTEXT_NAME",
                                "stop-${FailGoodJunitTestEngineConstants.DISPLAY_NAME}"))
                }
                it("sends one skip event and no start event for skipped tests") {
                    val testDescriptor =
                        engine.discover(
                            launcherDiscoveryRequest(
                                listOf(
                                    DiscoverySelectors.selectClass(
                                        IgnoredTestFixture::class.qualifiedName))),
                            UniqueId.forEngine(engine.id))
                    val listener = RememberingExecutionListener()
                    engine.execute(
                        ExecutionRequest.create(
                            testDescriptor,
                            listener,
                            NoConfigurationParameters(),
                            NoOutputDirectories()))
                    assert(
                        listener.list.toList() ==
                            listOf(
                                "start-FailGood",
                                "start-${IgnoredTestFixture::class.simpleName}: root context",
                                "skip-pending test-ignore-reason",
                                "stop-${IgnoredTestFixture::class.simpleName}: root context",
                                "stop-FailGood"))
                }
            }
        }
}

class NoOutputDirectories : OutputDirectoryProvider {
    override fun getRootDirectory(): Path {
        TODO("Not yet implemented")
    }

    override fun createOutputDirectory(testDescriptor: TestDescriptor?): Path {
        TODO("Not yet implemented")
    }
}

class NoConfigurationParameters : ConfigurationParameters {
    override fun get(key: String?): Optional<String> = Optional.empty()

    override fun getBoolean(key: String?): Optional<Boolean> = Optional.empty()

    @Deprecated("") override fun size(): Int = 0

    override fun keySet(): MutableSet<String> = mutableSetOf()
}

private fun <T> List<T>.replace(toReplace: Set<T>, with: T) =
    this.map { if (toReplace.contains(it)) with else it }

class RememberingExecutionListener : EngineExecutionListener {
    val list = ConcurrentLinkedQueue<String>()

    override fun executionStarted(testDescriptor: TestDescriptor) {
        list.add("start-${testDescriptor.displayName}")
    }

    override fun executionFinished(
        testDescriptor: TestDescriptor,
        testExecutionResult: TestExecutionResult?
    ) {
        list.add("stop-${testDescriptor.displayName}")
    }

    override fun executionSkipped(testDescriptor: TestDescriptor, reason: String?) {
        list.add("skip-${testDescriptor.displayName}-$reason")
    }
}
