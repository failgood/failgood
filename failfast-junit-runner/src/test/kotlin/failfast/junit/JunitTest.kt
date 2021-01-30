package failfast.junit

import failfast.describe
import org.junit.platform.commons.annotation.Testable
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory

@Testable
object JunitTest {
    val context = describe("The Junit Runner") {
        it("probably does something") {
            val discoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .filters(EngineFilter.includeEngines(FailFastJunitTestEngineConstants.id))
                .selectors(DiscoverySelectors.selectClass(MyTestClass::class.qualifiedName))
                .build()

            LauncherFactory.create().execute(discoveryRequest)
        }
    }
}

object Listener : TestExecutionListener {
    override fun testPlanExecutionStarted(testPlan: TestPlan?) {

    }

    override fun testPlanExecutionFinished(testPlan: TestPlan?) {
        super.testPlanExecutionFinished(testPlan)
    }

    override fun dynamicTestRegistered(testIdentifier: TestIdentifier?) {
        super.dynamicTestRegistered(testIdentifier)
    }

    override fun executionSkipped(testIdentifier: TestIdentifier?, reason: String?) {
        super.executionSkipped(testIdentifier, reason)
    }

    override fun executionStarted(testIdentifier: TestIdentifier?) {
        super.executionStarted(testIdentifier)
    }

    override fun executionFinished(testIdentifier: TestIdentifier?, testExecutionResult: TestExecutionResult?) {
        super.executionFinished(testIdentifier, testExecutionResult)
    }

    override fun reportingEntryPublished(testIdentifier: TestIdentifier?, entry: ReportEntry?) {
        super.reportingEntryPublished(testIdentifier, entry)
    }
}

object MyTestClass {
    val context = describe("root context") {
        it("contains a test") {}
    }
}
