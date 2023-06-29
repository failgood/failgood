package failgood.junit.delegate

import failgood.junit.FailGoodJunitTestEngine
import failgood.junit.FailGoodJunitTestEngineConstants
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.UniqueId

class DelegatingJunitEngine : TestEngine {
    override fun getId(): String = FailGoodJunitTestEngineConstants.id

    val currentEngine = FailGoodJunitTestEngine()

    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        return currentEngine.discover(discoveryRequest, uniqueId)
    }

    override fun execute(request: ExecutionRequest) {
        return currentEngine.execute(request)
    }
}
