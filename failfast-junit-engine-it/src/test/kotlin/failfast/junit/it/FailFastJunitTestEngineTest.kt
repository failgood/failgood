package failfast.junit.it

import failfast.describe
import failfast.junit.FailFastJunitTestEngine
import failfast.junit.FailFastJunitTestEngineConstants
import failfast.junit.it.fixtures.TestFixtureTest
import failfast.junit.it.fixtures.TestWithNestedContextsTest
import failfast.junit.it.fixtures.TestWithNestedContextsTest.CHILD_CONTEXT_1_NAME
import failfast.junit.it.fixtures.TestWithNestedContextsTest.CHILD_CONTEXT_2_NAME
import failfast.junit.it.fixtures.TestWithNestedContextsTest.ROOT_CONTEXT_NAME
import failfast.junit.it.fixtures.TestWithNestedContextsTest.TEST2_NAME
import failfast.junit.it.fixtures.TestWithNestedContextsTest.TEST_NAME
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import strikt.assertions.single
import java.util.concurrent.ConcurrentLinkedQueue

object FailFastJunitTestEngineTest {
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
            val testDescriptor =
                engine.discover(
                    launcherDiscoveryRequest(TestWithNestedContextsTest::class),
                    UniqueId.forEngine(engine.id)
                )

            it("starts and stops contexts in the correct order") {
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

}
