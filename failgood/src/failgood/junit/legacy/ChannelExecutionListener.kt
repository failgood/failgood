package failgood.junit.legacy

import failgood.ExecutionListener
import failgood.TestDescription
import failgood.TestPlusResult
import kotlinx.coroutines.channels.Channel

internal class ChannelExecutionListener : ExecutionListener {
    sealed class TestExecutionEvent {
        abstract val testDescription: TestDescription

        data class Started(override val testDescription: TestDescription) : TestExecutionEvent()

        data class Stopped(
            override val testDescription: TestDescription,
            val testResult: TestPlusResult
        ) : TestExecutionEvent()

        data class TestEvent(
            override val testDescription: TestDescription,
            val type: String,
            val payload: String
        ) : TestExecutionEvent()
    }

    val events = Channel<TestExecutionEvent>(Channel.UNLIMITED)

    override suspend fun testStarted(testDescription: TestDescription) {
        events.send(TestExecutionEvent.Started(testDescription))
    }

    override suspend fun testFinished(testPlusResult: TestPlusResult) {
        events.send(TestExecutionEvent.Stopped(testPlusResult.test, testPlusResult))
    }

    override suspend fun testEvent(
        testDescription: TestDescription,
        type: String,
        payload: String
    ) {
        events.send(TestExecutionEvent.TestEvent(testDescription, type, payload))
    }
}
