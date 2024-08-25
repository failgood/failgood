package failgood.internal.execution

import failgood.Context
import failgood.Test
import failgood.internal.given.RootGivenDSLHandler
import failgood.mock.mock
import failgood.testCollection
import kotlinx.coroutines.coroutineScope

@Test
object ContextVisitorTest {
    val tests = testCollection {
        it("can be easily created") {
            coroutineScope {
                val staticConfig = StaticContextExecutionConfig({}, this, givenFunction = {})
                ContextVisitor(
                    staticConfig,
                    ContextStateCollector(staticConfig, false),
                    Context("root"),
                    mock(),
                    executeAll = false,
                    onlyRunSubcontexts = false,
                    rootContextStartTime = 0,
                    givenDSL = RootGivenDSLHandler {})
            }
        }
    }
}
