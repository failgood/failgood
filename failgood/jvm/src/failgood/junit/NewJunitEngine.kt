package failgood.junit

import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.UniqueId

class NewJunitEngine : TestEngine {
    override fun getId(): String = "failgood-new"

    override fun discover(discoveryRequest: EngineDiscoveryRequest?, uniqueId: UniqueId?): TestDescriptor {
        TODO("Not yet implemented")
    }

    override fun execute(request: ExecutionRequest?) {
        TODO("Not yet implemented")
    }
}
