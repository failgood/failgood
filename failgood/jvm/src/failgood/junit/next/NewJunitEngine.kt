package failgood.junit.next

import failgood.Context
import failgood.CouldNotLoadContext
import failgood.ExecutionListener
import failgood.LoadResult
import failgood.RootContext
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.internal.LoadResults
import failgood.internal.SuiteExecutionContext
import failgood.internal.util.StringUniquer
import failgood.junit.ContextFinder
import failgood.junit.FailGoodJunitTestEngineConstants
import failgood.junit.FailGoodTestDescriptor
import failgood.junit.appendContext
import failgood.junit.createClassSource
import kotlinx.coroutines.runBlocking
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
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

        val uniqueMaker = StringUniquer()
        val mapper = mutableMapOf<LoadResult, FailGoodTestDescriptor>()

        val descriptors =
            loadResults.loadResults.map { context ->
                when (context) {
                    is RootContext -> {
                        val path =
                            uniqueMaker.makeUnique(
                                "${context.name}(${(context.sourceInfo.className)})"
                            )
                        val contextUniqueId = uniqueId.appendContext(path)

                        FailGoodTestDescriptor(
                            TestDescriptor.Type.CONTAINER,
                            contextUniqueId,
                            context.name,
                            createClassSource(context.sourceInfo)
                        )
                    }
                    is CouldNotLoadContext ->
                        FailGoodTestDescriptor(
                            TestDescriptor.Type.CONTAINER,
                            uniqueId.appendContext(context.jClass.name),
                            context.jClass.name,
                            null
                        )
                }.also { mapper[context] = it }
            }
        return FailGoodEngineDescriptor(uniqueId, id, loadResults, suiteExecutionContext, mapper)
            .apply { descriptors.forEach { this.addChild(it) } }
    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailGoodEngineDescriptor) return
        root.loadResults.investigate(
            root.suiteExecutionContext.scope,
            listener = NewExecutionListener(root.mapper)
        )
        TODO("Not yet implemented")
    }
    internal class FailGoodEngineDescriptor(
        uniqueId: UniqueId?,
        displayName: String?,
        val loadResults: LoadResults,
        val suiteExecutionContext: SuiteExecutionContext,
        val mapper: MutableMap<LoadResult, FailGoodTestDescriptor>
    ) : EngineDescriptor(uniqueId, displayName)
}

internal class NewExecutionListener(val mapper: MutableMap<LoadResult, FailGoodTestDescriptor>) :
    ExecutionListener {
        override suspend fun testDiscovered(testDescription: TestDescription) {
            TODO("Not yet implemented")
        }

        override suspend fun contextDiscovered(context: Context) {
            TODO("Not yet implemented")
        }

        override suspend fun testStarted(testDescription: TestDescription) {
            TODO("Not yet implemented")
        }

        override suspend fun testFinished(testPlusResult: TestPlusResult) {
            TODO("Not yet implemented")
        }

        override suspend fun testEvent(
            testDescription: TestDescription,
            type: String,
            payload: String
        ) {
            TODO("Not yet implemented")
        }
    }
