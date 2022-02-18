package failgood.junit

import failgood.Context
import failgood.SourceInfo
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
    val testSource = createFileSource(this.sourceInfo)
    return FailGoodTestDescriptor(
        TestDescriptor.Type.TEST,
        uniqueId.appendTest(testName),
        this.testName,
        testSource
    )
}

private fun createFileSource(sourceInfo: SourceInfo): TestSource? {
    val className = sourceInfo.className
    val filePosition = FilePosition.from(sourceInfo.lineNumber)
    val file = File("src/test/kotlin/${className.substringBefore("$").replace(".", "/")}.kt")
    return if (file.exists())
        FileSource.from(
            file,
            filePosition
        )
    else ClassSource.from(className, filePosition)
}

private fun createClassSource(sourceInfo: SourceInfo): TestSource? {
    val className = sourceInfo.className
    val filePosition = FilePosition.from(sourceInfo.lineNumber)
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
                        uniqueMaker.makeUnique("${context.name}(${(context.sourceInfo?.className) ?: ""})")
                    else
                        context.name
                    val contextUniqueId = uniqueId.appendContext(path)
                    val contextNode = FailGoodTestDescriptor(
                        TestDescriptor.Type.CONTAINER,
                        contextUniqueId,
                        context.name,
                        context.sourceInfo?.let {
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
                val path = "${context.name}(${(context.sourceInfo?.className) ?: ""})"
                val testDescriptor = FailGoodTestDescriptor(
                    TestDescriptor.Type.TEST,
                    uniqueId.appendContext(uniqueMaker.makeUnique(path)),
                    context.name, context.sourceInfo?.let { createClassSource(it) }
                )
                engineDescriptor.addChild(testDescriptor)
                mapper.addMapping(context, testDescriptor)
                engineDescriptor.failedContexts.add(contextInfo)
            }
        }
    }
    return engineDescriptor
}

private fun UniqueId.appendContext(path: String): UniqueId = append(CONTEXT_SEGMENT_TYPE, path.replace(" ", "."))
private fun UniqueId.appendTest(path: String): UniqueId = append(TEST_SEGMENT_TYPE, path.replace(" ", "."))
