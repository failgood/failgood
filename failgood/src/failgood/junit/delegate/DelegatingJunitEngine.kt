package failgood.junit.delegate

import failgood.junit.FailGoodJunitTestEngineConstants
import failgood.junit.FailGoodJunitTestEngineConstants.CONFIG_KEY_NEW_JUNIT
import failgood.junit.JunitEngine
import failgood.junit.legacy.LegacyJUnitTestEngine
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.UniqueId

class DelegatingJunitEngine : TestEngine {
    override fun getId(): String = FailGoodJunitTestEngineConstants.ID

    private lateinit var currentEngine: TestEngine

    override fun discover(
        discoveryRequest: EngineDiscoveryRequest,
        uniqueId: UniqueId
    ): TestDescriptor {
        val useNew =
            discoveryRequest.configurationParameters.getBoolean(CONFIG_KEY_NEW_JUNIT).orElse(true)
        currentEngine = if (useNew) JunitEngine() else LegacyJUnitTestEngine()
        return currentEngine.discover(discoveryRequest, uniqueId)
    }

    override fun execute(request: ExecutionRequest) {
        return currentEngine.execute(request)
    }
}
