package failgood.junit

import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.reporting.ReportEntry

internal class FailureLoggingEngineExecutionListener(private val delegate: EngineExecutionListener, private val failureLogger: FailureLogger) :
    EngineExecutionListener {
    override fun dynamicTestRegistered(testDescriptor: TestDescriptor?) {
        failureLogger.unsafe{delegate.dynamicTestRegistered(testDescriptor)}
    }

    override fun executionSkipped(testDescriptor: TestDescriptor?, reason: String?) {
        failureLogger.unsafe{delegate.executionSkipped(testDescriptor, reason)}
    }

    override fun executionStarted(testDescriptor: TestDescriptor?) {
        failureLogger.unsafe{delegate.executionStarted(testDescriptor)}
    }

    override fun executionFinished(testDescriptor: TestDescriptor?, testExecutionResult: TestExecutionResult?) {
        failureLogger.unsafe{delegate.executionFinished(testDescriptor, testExecutionResult)}
    }

    override fun reportingEntryPublished(testDescriptor: TestDescriptor?, entry: ReportEntry?) {
        failureLogger.unsafe{delegate.reportingEntryPublished(testDescriptor, entry)}
    }

}
