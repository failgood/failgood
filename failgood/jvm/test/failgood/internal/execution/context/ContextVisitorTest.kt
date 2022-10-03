package failgood.internal.execution.context

import failgood.Context
import failgood.Test
import failgood.describe
import failgood.mock.mock
import kotlinx.coroutines.coroutineScope

@Test
object ContextVisitorTest {
    val tests = describe<ContextVisitor<*>> {
        it("can be easily created") {
            coroutineScope {
                val staticConfig = StaticContextExecutionConfig({}, this)
                ContextVisitor(
                    staticConfig = staticConfig,
                    contextStateCollector = ContextStateCollector(staticConfig, false),
                    context = Context("root"),
                    given = {},
                    resourcesCloser = mock(),
                    onlyRunSubcontexts = false,
                    rootContextStartTime = 0
                )
            }
        }
    }
}