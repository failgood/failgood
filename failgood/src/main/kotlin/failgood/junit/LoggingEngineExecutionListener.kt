package failgood.junit

import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.reporting.ReportEntry

class LoggingEngineExecutionListener(private val delegate: EngineExecutionListener) : EngineExecutionListener {
    @Suppress("MemberVisibilityCanBePrivate")
    val events = mutableListOf<String>()
    private fun event(s: String) {
        events.add(s)
    }

    override fun executionSkipped(testDescriptor: TestDescriptor?, reason: String?) {
        event("executionSkipped " + name(testDescriptor))
        delegate.executionSkipped(testDescriptor, reason)
    }

    override fun executionStarted(testDescriptor: TestDescriptor?) {
        event("executionStarted " + name(testDescriptor))
        delegate.executionStarted(testDescriptor)
    }

    override fun executionFinished(testDescriptor: TestDescriptor?, testExecutionResult: TestExecutionResult?) {
        event("executionFinished " + name(testDescriptor) + ": " + testExecutionResult)
        delegate.executionFinished(testDescriptor, testExecutionResult)
    }

    override fun dynamicTestRegistered(testDescriptor: TestDescriptor?) {
        event("dynamicTestRegistered " + name(testDescriptor))
        delegate.dynamicTestRegistered(testDescriptor)
    }

    override fun reportingEntryPublished(testDescriptor: TestDescriptor?, entry: ReportEntry?) {
        event("reportingEntryPublished" + name(testDescriptor) + entry)
        delegate.reportingEntryPublished(testDescriptor, entry)
    }

    private fun name(testDescriptor: TestDescriptor?) = "${testDescriptor!!.displayName}(${testDescriptor.uniqueId})"
}

