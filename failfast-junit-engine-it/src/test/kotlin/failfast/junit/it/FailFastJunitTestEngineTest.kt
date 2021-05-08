package failfast.junit.it

import failfast.describe
import failfast.junit.FailFastJunitTestEngine
import failfast.junit.FailFastJunitTestEngineConstants
import failfast.junit.it.fixtures.PendingTestFixtureTest
import failfast.junit.it.fixtures.TestFixtureTest
import failfast.junit.it.fixtures.TestWithNestedContextsTest
import failfast.junit.it.fixtures.TestWithNestedContextsTest.CHILD_CONTEXT_1_NAME
import failfast.junit.it.fixtures.TestWithNestedContextsTest.CHILD_CONTEXT_2_NAME
import failfast.junit.it.fixtures.TestWithNestedContextsTest.ROOT_CONTEXT_NAME
import failfast.junit.it.fixtures.TestWithNestedContextsTest.TEST2_NAME
import failfast.junit.it.fixtures.TestWithNestedContextsTest.TEST_NAME
import org.junit.platform.commons.annotation.Testable
import org.junit.platform.engine.*
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import strikt.assertions.single
import java.util.concurrent.ConcurrentLinkedQueue

@Testable
class FailFastJunitTestEngineTest {
    val context = describe(FailFastJunitTestEngine::class) {
        val engine = FailFastJunitTestEngine()
        describe("can discover tests") {
            val testDescriptor =
                engine.discover(launcherDiscoveryRequest(TestFixtureTest::class), UniqueId.forEngine(engine.id))
            it("returns a root descriptor") {
                expectThat(testDescriptor.isRoot)
                expectThat(testDescriptor.displayName).isEqualTo("FailFast")
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
                        launcherDiscoveryRequest(TestWithNestedContextsTest::class),
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
                        "start-${FailFastJunitTestEngineConstants.displayName}",
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
                        "stop-${FailFastJunitTestEngineConstants.displayName}"
                    )
                )

            }
            it("sends one skip event and no start event for skipped tests") {
                val testDescriptor =
                    engine.discover(
                        launcherDiscoveryRequest(PendingTestFixtureTest::class),
                        UniqueId.forEngine(engine.id)
                    )
                val listener = RememberingExecutionListener()
                engine.execute(ExecutionRequest(testDescriptor, listener, null))
                expectThat(listener.list.toList()).isEqualTo(
                    listOf(
                        "start-FailFast",
                        "start-the root context",
                        "skip-pending test",
                        "stop-the root context",
                        "stop-FailFast"
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
