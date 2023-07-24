package failgood.junit.delegate

import failgood.junit.FailGoodJunitTestEngine
import failgood.junit.FailGoodJunitTestEngineConstants
import failgood.junit.next.NewJunitEngine
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.UniqueId

class DelegatingJunitEngine : TestEngine {
    override fun getId(): String = FailGoodJunitTestEngineConstants.id

    private lateinit var currentEngine: TestEngine
    override fun discover(
        discoveryRequest: EngineDiscoveryRequest,
        uniqueId: UniqueId
    ): TestDescriptor {
        val useNew =
            discoveryRequest.configurationParameters.getBoolean("failgood.new.junit").orElse(false)
        currentEngine = if (useNew) NewJunitEngine() else FailGoodJunitTestEngine()
        return currentEngine.discover(discoveryRequest, uniqueId)
    }

    override fun execute(request: ExecutionRequest) {
        return currentEngine.execute(request)
    }
}
