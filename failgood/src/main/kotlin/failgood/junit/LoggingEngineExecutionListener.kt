package failgood.junit

import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult

class LoggingEngineExecutionListener(private val delegate: EngineExecutionListener) : EngineExecutionListener {
    val events = mutableListOf<String>()
    private fun event(s: String) {
        events.add(s)
    }

    override fun executionSkipped(testDescriptor: TestDescriptor?, reason: String?) {
        event("executionSkipped " + testDescriptor!!.displayName)
        delegate.executionSkipped(testDescriptor, reason)
    }

    override fun executionStarted(testDescriptor: TestDescriptor?) {
        event("executionStarted " + testDescriptor!!.displayName)
        delegate.executionStarted(testDescriptor)
    }

    override fun executionFinished(testDescriptor: TestDescriptor?, testExecutionResult: TestExecutionResult?) {
        event("executionFinished " + testDescriptor!!.displayName + ": " + testExecutionResult)
        delegate.executionFinished(testDescriptor, testExecutionResult)
    }

}
