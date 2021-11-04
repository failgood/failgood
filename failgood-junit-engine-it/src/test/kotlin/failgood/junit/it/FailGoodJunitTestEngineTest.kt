package failgood.junit.it

import failgood.Test
import failgood.describe
import failgood.junit.FailGoodJunitTestEngine
import failgood.junit.FailGoodJunitTestEngineConstants
import failgood.junit.it.fixtures.PendingTestFixtureTest
import failgood.junit.it.fixtures.TestFixtureTest
import failgood.junit.it.fixtures.TestWithNestedContextsTest
import failgood.junit.it.fixtures.TestWithNestedContextsTest.Companion.CHILD_CONTEXT_1_NAME
import failgood.junit.it.fixtures.TestWithNestedContextsTest.Companion.CHILD_CONTEXT_2_NAME
import failgood.junit.it.fixtures.TestWithNestedContextsTest.Companion.ROOT_CONTEXT_NAME
import failgood.junit.it.fixtures.TestWithNestedContextsTest.Companion.TEST2_NAME
import failgood.junit.it.fixtures.TestWithNestedContextsTest.Companion.TEST_NAME
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.DiscoverySelectors
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import strikt.assertions.single
import java.util.concurrent.ConcurrentLinkedQueue

@Test
class FailGoodJunitTestEngineTest {
    val context = describe(FailGoodJunitTestEngine::class) {
        val engine = FailGoodJunitTestEngine()
        describe("can discover tests") {
            val testDescriptor =
                engine.discover(
                    launcherDiscoveryRequest(DiscoverySelectors.selectClass(TestFixtureTest::class.qualifiedName)),
                    UniqueId.forEngine(engine.id)
                )
            it("returns a root descriptor") {
                expectThat(testDescriptor.isRoot)
                expectThat(testDescriptor.displayName).isEqualTo("FailGood")
            }
            it("returns all root contexts") {
                expectThat(testDescriptor.children).single().and {
                    get { isContainer }.isTrue()
                    get { displayName }.isEqualTo(TestFixtureTest.ROOT_CONTEXT_NAME)
                }
            }
        }
        describe("test execution") {

            it("starts and stops contexts in the correct order") {
                val testDescriptor =
                    engine.discover(
                        launcherDiscoveryRequest(DiscoverySelectors.selectClass(TestWithNestedContextsTest::class.qualifiedName)),
                        UniqueId.forEngine(engine.id)
                    )
                val listener = RememberingExecutionListener()
                engine.execute(ExecutionRequest(testDescriptor, listener, null))
                expectThat(
                    listener.list.toList()
                        .replace(
                            // we don't know in what order the tests will run
                            setOf(
                                "start-$TEST_NAME",
                                "stop-$TEST_NAME",
                                "start-$TEST2_NAME",
                                "stop-$TEST2_NAME"
                            ), "some-test-event"
                        )
                ).isEqualTo(
                    listOf(
                        "start-${FailGoodJunitTestEngineConstants.displayName}",
                        "start-$ROOT_CONTEXT_NAME",
                        "start-$CHILD_CONTEXT_1_NAME",
                        "start-$CHILD_CONTEXT_2_NAME",
                        "some-test-event",
                        "some-test-event",
                        "some-test-event",
                        "some-test-event",
                        "stop-$CHILD_CONTEXT_2_NAME",
                        "stop-$CHILD_CONTEXT_1_NAME",
                        "stop-$ROOT_CONTEXT_NAME",
                        "stop-${FailGoodJunitTestEngineConstants.displayName}"
                    )
                )

            }
            it("sends one skip event and no start event for skipped tests") {
                val testDescriptor =
                    engine.discover(
                        launcherDiscoveryRequest(DiscoverySelectors.selectClass(PendingTestFixtureTest::class.qualifiedName)),
                        UniqueId.forEngine(engine.id)
                    )
                val listener = RememberingExecutionListener()
                engine.execute(ExecutionRequest(testDescriptor, listener, null))
                expectThat(listener.list.toList()).isEqualTo(
                    listOf(
                        "start-FailGood",
                        "start-the root context",
                        "skip-pending test",
                        "stop-the root context",
                        "stop-FailGood"
                    )
                )

            }

        }
    }
}

private fun <T> List<T>.replace(toReplace: Set<T>, with: T) = this.map { if (toReplace.contains(it)) with else it }

class RememberingExecutionListener : EngineExecutionListener {
    val list = ConcurrentLinkedQueue<String>()
    override fun executionStarted(testDescriptor: TestDescriptor) {
        list.add("start-${testDescriptor.displayName}")
    }

    override fun executionFinished(testDescriptor: TestDescriptor, testExecutionResult: TestExecutionResult?) {
        list.add("stop-${testDescriptor.displayName}")
    }

    override fun executionSkipped(testDescriptor: TestDescriptor, reason: String?) {
        list.add("skip-${testDescriptor.displayName}")
    }
}
