package failgood.junit.legacy

import failgood.Context
import failgood.TestDescription
import failgood.internal.FailedTestCollectionExecution
import failgood.internal.TestCollectionExecutionResult
import failgood.internal.TestResults
import failgood.internal.util.StringUniquer
import failgood.junit.createClassSource
import failgood.junit.createFileSource
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId

internal fun TestDescription.toTestDescriptor(uniqueId: UniqueId): TestDescriptor {
    val testSource = createFileSource(this.sourceInfo, this.testName)
    return FailGoodTestDescriptor(
        TestDescriptor.Type.TEST, uniqueId.appendTest(testName), this.testName, testSource)
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
                                "${context.name}(${(context.sourceInfo?.className) ?: ""})")
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
                            })
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
                        context.sourceInfo?.let { createClassSource(it) })
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
