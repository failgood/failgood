package failgood.junit

import failgood.*
import failgood.ExecutionListener
import failgood.Skipped
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSource
import org.junit.platform.engine.support.descriptor.MethodSource
import java.io.File

internal class ExecutionListener(
    private val root: JunitEngine.FailGoodEngineDescriptor,
    private val listener: EngineExecutionListener,
    private val startedContexts: MutableSet<Context>,
    private val testMapper: TestMapper
) : ExecutionListener {
    override suspend fun testDiscovered(testDescription: TestDescription) {
        /*
        Every event handler in this class is synchronized for now because we must make sure that
        events have the correct order. This should be optimized at some point now that everthing is stable
        */
        synchronized(this) {
            val parent = testMapper.getMapping(testDescription.context)
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
            val node = TestPlanNode.Container(context.name, context.displayName)
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
        val context = testDescription.context
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

private val fs = File.separator

// Roots for guessing source files.
// It's ok if this fails.
// If we don't find the source file, "navigate to source" in IDEAs junit runner does not work.
private val sourceRoots: List<String> =
    listOf("src${fs}test${fs}kotlin", "src${fs}test${fs}java", "test", "jvm${fs}test")

internal fun createFileSource(sourceInfo: SourceInfo, testOrContextName: String): TestSource? {
    val className = sourceInfo.className
    val filePosition = FilePosition.from(sourceInfo.lineNumber)
    val classFilePath = "${className.substringBefore("$").replace(".", "/")}.kt"
    val file = sourceRoots.asSequence().map { File("$it/$classFilePath") }.firstOrNull(File::exists)
    return if (file != null) FileSource.from(file, filePosition)
    else MethodSource.from(className, testOrContextName.replace(" ", "+"))
}

internal fun createClassSource(sourceInfo: SourceInfo): TestSource? {
    val className = sourceInfo.className
    val filePosition = FilePosition.from(sourceInfo.lineNumber)
    return ClassSource.from(className, filePosition)
}
