package failgood.junit

import failgood.Context
import failgood.TestDescription
import failgood.internal.ContextInfo
import failgood.internal.ContextResult
import failgood.internal.FailedRootContext
import failgood.internal.SourceInfo
import failgood.util.StringUniquer
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSource
import org.junit.platform.engine.support.descriptor.MethodSource
import java.io.File

private fun TestDescription.toTestDescriptor(uniqueId: UniqueId): TestDescriptor {
    val testSource = createFileSource(this.sourceInfo, this.testName)
    return FailGoodTestDescriptor(
        TestDescriptor.Type.TEST,
        uniqueId.appendTest(testName),
        this.testName,
        testSource
    )
}
private val FS = File.separator

// roots for guessing source files. Its ok if this fails.
// if we don't find the source file, navigating to source in idea does not work.
private val sourceRoots: List<String> =
    listOf("src${FS}test${FS}kotlin", "src${FS}test${FS}java", "test", "jvm${FS}test")

private fun createFileSource(sourceInfo: SourceInfo, testOrContextName: String): TestSource? {
    val className = sourceInfo.className
    val filePosition = FilePosition.from(sourceInfo.lineNumber)
    val classFilePath = "${className.substringBefore("$").replace(".", "/")}.kt"
    val file = sourceRoots.asSequence().map { File("$it/$classFilePath") }.firstOrNull(File::exists)
    return if (file != null)
        FileSource.from(
            file,
            filePosition
        )
    else MethodSource.from(className, testOrContextName.replace(" ", "+"))
}

private fun createClassSource(sourceInfo: SourceInfo): TestSource? {
    val className = sourceInfo.className
    val filePosition = FilePosition.from(sourceInfo.lineNumber)
    return ClassSource.from(className, filePosition)
}

internal fun createResponse(
    uniqueId: UniqueId,
    contextInfos: List<ContextResult>,
    failGoodEngineDescriptor: FailGoodEngineDescriptor
): FailGoodEngineDescriptor {
    val uniqueMaker = StringUniquer()
    val mapper = failGoodEngineDescriptor.mapper
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
                                createFileSource(it, context.name)
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
                    addChildren(failGoodEngineDescriptor, rootContext, true, uniqueId)
            }

            is FailedRootContext -> {
                val context = contextInfo.context
                val path = "${context.name}(${(context.sourceInfo?.className) ?: ""})"
                val testDescriptor = FailGoodTestDescriptor(
                    TestDescriptor.Type.TEST,
                    uniqueId.appendContext(uniqueMaker.makeUnique(path)),
                    context.name, context.sourceInfo?.let { createClassSource(it) }
                )
                failGoodEngineDescriptor.addChild(testDescriptor)
                mapper.addMapping(context, testDescriptor)
                failGoodEngineDescriptor.failedRootContexts.add(contextInfo)
            }
        }
    }
    return failGoodEngineDescriptor
}

private fun UniqueId.appendContext(path: String): UniqueId = append(CONTEXT_SEGMENT_TYPE, path)
private fun UniqueId.appendTest(path: String): UniqueId = append(TEST_SEGMENT_TYPE, path)
