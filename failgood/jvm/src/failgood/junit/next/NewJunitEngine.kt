package failgood.junit.next

import failgood.Context
import failgood.ExecutionListener
import failgood.Failure
import failgood.Skipped
import failgood.Success
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.internal.LoadResults
import failgood.internal.SuiteExecutionContext
import failgood.junit.ContextFinder
import failgood.junit.FailGoodJunitTestEngineConstants
import failgood.junit.TestMapper
import failgood.junit.exp.DynamicTestDescriptor
import failgood.junit.exp.TestPlanNode
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor

class NewJunitEngine : TestEngine {
    override fun getId(): String = "failgood-new"

    override fun discover(
        discoveryRequest: EngineDiscoveryRequest,
        uniqueId: UniqueId
    ): TestDescriptor {
        val suiteExecutionContext = SuiteExecutionContext()

        val runTestFixtures =
            discoveryRequest.configurationParameters
                .getBoolean(FailGoodJunitTestEngineConstants.RUN_TEST_FIXTURES)
                .orElse(false)
        val suiteAndFilters = ContextFinder(runTestFixtures).findContexts(discoveryRequest)
        suiteAndFilters
            ?: return EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.displayName)
        val loadResults =
            runBlocking(suiteExecutionContext.coroutineDispatcher) {
                suiteAndFilters.suite.getRootContexts(suiteExecutionContext.scope)
            }

//        val uniqueMaker = StringUniquer()
        /*
                val descriptors =
                    loadResults.loadResults.map { rootContext ->
                        when (rootContext) {
                            is RootContext -> {
                                val path =
                                    uniqueMaker.makeUnique(
                                        "${rootContext.context.name}(${(rootContext.context.sourceInfo!!.className)})"
                                    )
                                val contextUniqueId = uniqueId.appendContext(path)

                                FailGoodTestDescriptor(
                                    TestDescriptor.Type.CONTAINER,
                                    contextUniqueId,
                                    rootContext.context.name,
                                    createClassSource(rootContext.sourceInfo)
                                ).also { mapper[rootContext.context] = it }
                            }

                            is CouldNotLoadContext ->
                                FailGoodTestDescriptor(
                                    TestDescriptor.Type.CONTAINER,
                                    uniqueId.appendContext(rootContext.jClass.name),
                                    rootContext.jClass.name,
                                    null
                                )
                        }
                    }*/
        return FailGoodEngineDescriptor(uniqueId, id, loadResults, suiteExecutionContext)
//            .apply { descriptors.forEach { this.addChild(it) } }
    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailGoodEngineDescriptor) return
        try {
            runBlocking(root.suiteExecutionContext.coroutineDispatcher) {
                root.loadResults.investigate(
                    root.suiteExecutionContext.scope,
                    listener = NewExecutionListener(root, request.engineExecutionListener)
                ).awaitAll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal class FailGoodEngineDescriptor(
        uniqueId: UniqueId?,
        displayName: String?,
        val loadResults: LoadResults,
        val suiteExecutionContext: SuiteExecutionContext
    ) : EngineDescriptor(uniqueId, displayName)
}

internal class NewExecutionListener(
    private val root: NewJunitEngine.FailGoodEngineDescriptor,
    private val engineExecutionListener: EngineExecutionListener
) :
    ExecutionListener {
        val mapper = TestMapper()
        override suspend fun testDiscovered(testDescription: TestDescription) {
            val parent = mapper.getMapping(testDescription.container)
            val node = TestPlanNode.Test(testDescription.testName)
            val descriptor = DynamicTestDescriptor(node, parent)
            mapper.addMapping(testDescription, descriptor)

            engineExecutionListener.dynamicTestRegistered(descriptor)
        }

        override suspend fun contextDiscovered(context: Context) {
            val node = TestPlanNode.Container(context.name)
            val parent =
                if (context.parent == null) root else mapper.getMapping(context.parent)
            val descriptor = DynamicTestDescriptor(node, parent)
            mapper.addMapping(context, descriptor)

            engineExecutionListener.dynamicTestRegistered(descriptor)
            engineExecutionListener.executionStarted(descriptor)
        }

        override suspend fun testStarted(testDescription: TestDescription) {
            val descriptor = mapper.getMappingOrNull(testDescription)!!
            engineExecutionListener.executionStarted(descriptor)
        }

        override suspend fun testFinished(testPlusResult: TestPlusResult) {
            val descriptor = mapper.getMappingOrNull(testPlusResult.test)!!
            when (testPlusResult.result) {
                is Failure -> engineExecutionListener.executionFinished(
                    descriptor,
                    TestExecutionResult.failed(testPlusResult.result.failure)
                )

                is Skipped -> engineExecutionListener.executionSkipped(descriptor, testPlusResult.result.reason)
                is Success -> engineExecutionListener.executionFinished(descriptor, TestExecutionResult.successful())
            }
        }

        override suspend fun testEvent(testDescription: TestDescription, type: String, payload: String) {
        }
    }
