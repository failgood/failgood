package failfast.junit.it

import failfast.describe
import failfast.junit.FailFastJunitTestEngine
import failfast.junit.FailFastJunitTestEngineConstants
import failfast.junit.it.RememberingExecutionListener.What
import failfast.junit.it.fixtures.DuplicateTestNameTest
import failfast.junit.it.fixtures.TestWithNestedContextsTest
import failfast.junit.it.fixtures.TestWithNestedContextsTest.CHILD_CONTEXT_1_NAME
import failfast.junit.it.fixtures.TestWithNestedContextsTest.CHILD_CONTEXT_2_NAME
import failfast.junit.it.fixtures.TestWithNestedContextsTest.ROOT_CONTEXT_NAME
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
                engine.discover(launcherDiscoveryRequest(DuplicateTestNameTest::class), UniqueId.forEngine(engine.id))
            it("returns a root descriptor") {
                expectThat(testDescriptor.isRoot)
                expectThat(testDescriptor.displayName).isEqualTo("FailFast")
            }
            it("returns all root contexts") {
                expectThat(testDescriptor.children).single().and {
                    get { isContainer }.isTrue()
                    get { displayName }.isEqualTo(DuplicateTestNameTest.ROOT_CONTEXT_NAME)
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
                expectThat(listener.list.toList()).isEqualTo(
                    listOf(
                        RememberingExecutionListener.Event(What.START, FailFastJunitTestEngineConstants.displayName),
                        RememberingExecutionListener.Event(What.START, ROOT_CONTEXT_NAME),
                        RememberingExecutionListener.Event(What.START, CHILD_CONTEXT_1_NAME),
                        RememberingExecutionListener.Event(What.START, CHILD_CONTEXT_2_NAME),
                        RememberingExecutionListener.Event(What.START, TEST_NAME),
                        RememberingExecutionListener.Event(What.STOP, TEST_NAME),
                        RememberingExecutionListener.Event(What.STOP, CHILD_CONTEXT_2_NAME),
                        RememberingExecutionListener.Event(What.STOP, CHILD_CONTEXT_1_NAME),
                        RememberingExecutionListener.Event(What.STOP, ROOT_CONTEXT_NAME),
                        RememberingExecutionListener.Event(What.STOP, FailFastJunitTestEngineConstants.displayName),
                    )
                )

            }
        }
    }
}

class RememberingExecutionListener : EngineExecutionListener {
    enum class What {
        START, STOP
    }

    data class Event(val what: What, val name: String)

    val list = ConcurrentLinkedQueue<Event>()
    override fun executionStarted(testDescriptor: TestDescriptor) {
        list.add(Event(What.START, testDescriptor.displayName))
    }

    override fun executionFinished(testDescriptor: TestDescriptor, testExecutionResult: TestExecutionResult?) {
        list.add(Event(What.STOP, testDescriptor.displayName))
    }

}
