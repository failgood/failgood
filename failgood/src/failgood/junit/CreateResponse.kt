package failgood.junit

import failgood.Context
import failgood.SourceInfo
import failgood.TestDescription
import failgood.internal.TestResults
import failgood.internal.TestCollectionExecutionResult
import failgood.internal.FailedTestCollectionExecution
import failgood.internal.util.StringUniquer
import java.io.File
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSource
import org.junit.platform.engine.support.descriptor.MethodSource

internal fun TestDescription.toTestDescriptor(uniqueId: UniqueId): TestDescriptor {
    val testSource = createFileSource(this.sourceInfo, this.testName)
    return FailGoodTestDescriptor(
        TestDescriptor.Type.TEST,
        uniqueId.appendTest(testName),
        this.testName,
        testSource
    )
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

internal fun createResponse(
    uniqueId: UniqueId,
    contextInfos: List<TestCollectionExecutionResult>,
    failGoodEngineDescriptor: FailGoodEngineDescriptor
): FailGoodEngineDescriptor {
    val uniqueMaker = StringUniquer()
    val mapper = failGoodEngineDescriptor.mapper
    contextInfos.forEach { contextInfo ->
        when (contextInfo) {
            is TestResults -> {
                val tests = contextInfo.tests.entries
                fun addChildren(
                    node: TestDescriptor,
                    context: Context,
                    isRootContext: Boolean,
                    uniqueId: UniqueId
                ) {
                    val path =
                        if (isRootContext)
                            uniqueMaker.makeUnique(
                                "${context.name}(${(context.sourceInfo?.className) ?: ""})"
                            )
                        else context.name
                    val contextUniqueId = uniqueId.appendContext(path)
                    val contextNode =
                        FailGoodTestDescriptor(
                            TestDescriptor.Type.CONTAINER,
                            contextUniqueId,
                            context.displayName,
                            context.sourceInfo?.let {
                                if (isRootContext) createClassSource(it)
                                else createFileSource(it, context.name)
                            }
                        )
                    mapper.addMapping(context, contextNode)
                    val testsInThisContext = tests.filter { it.key.context == context }
                    testsInThisContext.forEach {
                        val testDescription = it.key
                        val testDescriptor = testDescription.toTestDescriptor(contextUniqueId)
                        contextNode.addChild(testDescriptor)
                        mapper.addMapping(testDescription, testDescriptor)
                    }
                    val contextsInThisContext = contextInfo.contexts.filter { it.parent == context }
                    contextsInThisContext.forEach {
                        addChildren(contextNode, it, false, contextUniqueId)
                    }
                    node.addChild(contextNode)
                }

                val rootContext = contextInfo.contexts.singleOrNull { it.parent == null }
                if (rootContext != null)
                    addChildren(failGoodEngineDescriptor, rootContext, true, uniqueId)
            }
            is FailedTestCollectionExecution -> {
                val context = contextInfo.context
                val path = "${context.name}(${(context.sourceInfo?.className) ?: ""})"
                val testDescriptor =
                    FailGoodTestDescriptor(
                        TestDescriptor.Type.TEST,
                        uniqueId.appendContext(uniqueMaker.makeUnique(path)),
                        context.displayName,
                        context.sourceInfo?.let { createClassSource(it) }
                    )
                failGoodEngineDescriptor.addChild(testDescriptor)
                mapper.addMapping(context, testDescriptor)
                failGoodEngineDescriptor.failedRootContexts.add(contextInfo)
            }
        }
    }
    return failGoodEngineDescriptor
}

internal fun UniqueId.appendContext(path: String): UniqueId = append(CONTEXT_SEGMENT_TYPE, path)

internal fun UniqueId.appendTest(path: String): UniqueId = append(TEST_SEGMENT_TYPE, path)
