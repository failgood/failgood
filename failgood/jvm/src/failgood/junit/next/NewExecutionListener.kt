package failgood.junit.next

import failgood.Context
import failgood.ExecutionListener
import failgood.FailGoodException
import failgood.Failure
import failgood.Skipped
import failgood.Success
import failgood.TestContainer
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.junit.TestMapper
import failgood.junit.createClassSource
import failgood.junit.createFileSource
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.reporting.ReportEntry

internal class NewExecutionListener(
    private val root: NewJunitEngine.FailGoodEngineDescriptor,
    private val listener: EngineExecutionListener,
    private val startedContexts: MutableSet<TestContainer>,
    private val testMapper: TestMapper
) : ExecutionListener {
    override suspend fun testDiscovered(testDescription: TestDescription) {
        /*
        Every event handler in this class is synchronized for now because we must make sure that
        events have the correct order.
        When everything is feature complete and stable we can see if we should optimize this.
        */
        synchronized(this) {
            val parent = testMapper.getMapping(testDescription.container)
            val node = TestPlanNode.Test(testDescription.testName)
            val descriptor =
                DynamicTestDescriptor(
                    node,
                    parent,
                    source = createFileSource(testDescription.sourceInfo, testDescription.testName)
                )
            testMapper.addMapping(testDescription, descriptor)
            listener.dynamicTestRegistered(descriptor)
        }
    }

    override suspend fun contextDiscovered(context: Context) {
        synchronized(this) {
            val node = TestPlanNode.Container(context.name)
            val descriptor =
                if (context.parent == null) {
                    DynamicTestDescriptor(
                        node,
                        root,
                        "${context.name}(${(context.sourceInfo?.className) ?: ""})",
                        context.sourceInfo?.let { createClassSource(it) }
                    )
                } else {
                    DynamicTestDescriptor(
                        node,
                        testMapper.getMapping(context.parent),
                        source = context.sourceInfo?.let { createFileSource(it, context.name) }
                    )
                }
            testMapper.addMapping(context, descriptor)
            listener.dynamicTestRegistered(descriptor)
        }
    }

    override suspend fun testStarted(testDescription: TestDescription) {
        synchronized(this) {
            val descriptor =
                testMapper.getMappingOrNull(testDescription)
                    ?: throw FailGoodException("mapping for $testDescription not found")
            startParentContexts(testDescription)
            listener.executionStarted(descriptor)
        }
    }

    private fun startParentContexts(testDescription: TestDescription) {
        val context = testDescription.container
        (context.parents + context).forEach {
            if (startedContexts.add(it)) {
                listener.executionStarted(testMapper.getMapping(it))
            }
        }
    }

    override suspend fun testFinished(testPlusResult: TestPlusResult) {
        synchronized(this) {
            val testDescription = testPlusResult.test
            val descriptor = testMapper.getMapping(testDescription)
            when (testPlusResult.result) {
                is Failure ->
                    listener.executionFinished(
                        descriptor,
                        TestExecutionResult.failed(testPlusResult.result.failure)
                    )
                is Skipped -> {
                    // for skipped tests testStarted is not called, so we have to start parent
                    // contexts
                    // here.
                    startParentContexts(testDescription)
                    listener.executionSkipped(descriptor, testPlusResult.result.reason)
                }
                is Success ->
                    listener.executionFinished(descriptor, TestExecutionResult.successful())
            }
        }
    }

    override suspend fun testEvent(
        testDescription: TestDescription,
        type: String,
        payload: String
    ) {
        listener.reportingEntryPublished(
            testMapper.getMapping(testDescription),
            ReportEntry.from(type, payload)
        )
    }
}
