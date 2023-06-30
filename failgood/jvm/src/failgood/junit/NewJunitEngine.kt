package failgood.junit

import failgood.CouldNotLoadContext
import failgood.RootContext
import failgood.internal.SuiteExecutionContext
import failgood.internal.util.StringUniquer
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

        val descriptors =
            loadResults.map { context ->
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
                }
            }
        return FailgoodTestDescriptor(uniqueId, id).apply {
            descriptors.forEach { this.addChild(it) }
        }
    }

    override fun execute(request: ExecutionRequest?) {
        TODO("Not yet implemented")
    }
    class FailgoodTestDescriptor(uniqueId: UniqueId?, displayName: String?) :
        EngineDescriptor(uniqueId, displayName)
}
