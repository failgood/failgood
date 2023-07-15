package failgood.junit.exp

import failgood.Ignored
import failgood.Test
import failgood.describe
import org.junit.platform.launcher.core.EngineDiscoveryOrchestrator
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder

@Test
class PlaygroundEngineTest {
    val context = describe<PlaygroundEngine> {
        it("works", ignored = Ignored.Because("working on it")) {
            val orchestrator = EngineDiscoveryOrchestrator(listOf(PlaygroundEngine()), listOf())
            orchestrator.discover(
                LauncherDiscoveryRequestBuilder.request()
                    .selectors(listOf())
                    .build(),
                EngineDiscoveryOrchestrator.Phase.DISCOVERY
            )
        }
    }
}
