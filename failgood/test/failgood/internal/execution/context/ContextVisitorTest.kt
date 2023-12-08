package failgood.internal.execution.context

import failgood.Context
import failgood.Test
import failgood.describe
import failgood.internal.given.RootGivenDSLHandler
import failgood.mock.mock
import kotlinx.coroutines.coroutineScope

@Test
object ContextVisitorTest {
    val tests = describe {
        it("can be easily created") {
            coroutineScope {
                val staticConfig = StaticContextExecutionConfig({}, this, given = {})
                ContextVisitor(
                    staticConfig,
                    ContextStateCollector(staticConfig, false),
                    Context("root"),
                    mock(),
                    executeAll = false,
                    onlyRunSubcontexts = false,
                    rootContextStartTime = 0,
                    givenDSL = RootGivenDSLHandler {}
                )
            }
        }
    }
}
