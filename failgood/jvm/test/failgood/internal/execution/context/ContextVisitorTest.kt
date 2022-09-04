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
                ContextVisitor(
                    staticConfig = StaticContextExecutionConfig({}, this),
                    contextStateCollector = ContextStateCollector(mock(), false),
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
