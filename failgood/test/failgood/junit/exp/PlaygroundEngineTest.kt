package failgood.junit.exp

import failgood.Test
import failgood.testsAbout
import org.junit.platform.launcher.core.EngineDiscoveryOrchestrator
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder

@Test
class PlaygroundEngineTest {
    val tests =
        testsAbout(PlaygroundEngine::class) {
            it("returns a valid test plan") {
                val orchestrator = EngineDiscoveryOrchestrator(listOf(PlaygroundEngine()), listOf())
                orchestrator.discover(
                    LauncherDiscoveryRequestBuilder.request().selectors(listOf()).build(),
                    EngineDiscoveryOrchestrator.Phase.DISCOVERY
                )
            }
        }
}
