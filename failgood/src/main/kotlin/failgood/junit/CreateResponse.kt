package failgood.junit

import failgood.Context
import failgood.TestDescription
import failgood.internal.ContextInfo
import failgood.internal.ContextResult
import failgood.internal.FailedContext
import failgood.util.StringUniquer
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSource
import java.io.File

private fun TestDescription.toTestDescriptor(uniqueId: UniqueId): TestDescriptor {
    val stackTraceElement = this.stackTraceElement
    val testSource = createFileSource(stackTraceElement)
    return FailGoodTestDescriptor(
        TestDescriptor.Type.TEST,
        uniqueId.append(TEST_SEGMENT_TYPE, testName),
        this.testName,
        testSource
    )
}

private fun createFileSource(stackTraceElement: StackTraceElement): TestSource? {
    val className = stackTraceElement.className
    val filePosition = FilePosition.from(stackTraceElement.lineNumber)
    val file = File("src/test/kotlin/${className.substringBefore("$").replace(".", "/")}.kt")
    return if (file.exists())
        FileSource.from(
            file,
            filePosition
        )
    else ClassSource.from(className, filePosition)
}

private fun createClassSource(stackTraceElement: StackTraceElement): TestSource? {
    val className = stackTraceElement.className
    val filePosition = FilePosition.from(stackTraceElement.lineNumber)
    return ClassSource.from(className, filePosition)
}

internal fun createResponse(
    uniqueId: UniqueId,
    contextInfos: List<ContextResult>,
    executionListener: JunitExecutionListener
): FailGoodEngineDescriptor {
    val uniqueMaker = StringUniquer()
    val engineDescriptor = FailGoodEngineDescriptor(uniqueId, contextInfos, executionListener)
    val mapper = engineDescriptor.mapper
    contextInfos.forEach { contextInfo ->
        when (contextInfo) {
            is ContextInfo -> {
                val tests = contextInfo.tests.entries
                fun addChildren(node: TestDescriptor, context: Context, isRootContext: Boolean, uniqueId: UniqueId) {
                    val path = if (isRootContext)
                        uniqueMaker.makeUnique(context.name)
                    else
                        context.name
                    val contextUniqueId = uniqueId.append(CONTEXT_SEGMENT_TYPE, path)
                    val contextNode = FailGoodTestDescriptor(
                        TestDescriptor.Type.CONTAINER,
                        contextUniqueId,
                        context.name,
                        context.stackTraceElement?.let {
                            if (isRootContext)
                                createClassSource(it)
                            else
                                createFileSource(it)
                        }
                    )
                    mapper.addMapping(context, contextNode)
                    val testsInThisContext = tests.filter { it.key.container == context }
                    testsInThisContext.forEach {
                        val testDescription = it.key
                        val testDescriptor = testDescription.toTestDescriptor(contextUniqueId)
                        contextNode.addChild(testDescriptor)
                        mapper.addMapping(testDescription, testDescriptor)
                    }
                    val contextsInThisContext = contextInfo.contexts.filter { it.parent == context }
                    contextsInThisContext.forEach { addChildren(contextNode, it, false, contextUniqueId) }
                    node.addChild(contextNode)
                }

                val rootContext = contextInfo.contexts.singleOrNull { it.parent == null }
                if (rootContext != null)
                    addChildren(engineDescriptor, rootContext, true, uniqueId)

            }
            is FailedContext -> {
                val context = contextInfo.context
                val testDescriptor = FailGoodTestDescriptor(TestDescriptor.Type.CONTAINER,
                    uniqueId.append(CONTEXT_SEGMENT_TYPE, uniqueMaker.makeUnique(context.stringPath())),
                    context.name, context.stackTraceElement?.let { createFileSource(it) })
                engineDescriptor.addChild(testDescriptor)
                mapper.addMapping(context, testDescriptor)
                engineDescriptor.failedContexts.add(contextInfo)
            }
        }
    }
    return engineDescriptor
}
