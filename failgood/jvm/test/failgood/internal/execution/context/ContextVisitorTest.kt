package failgood.internal.execution.context

import failgood.Context
import failgood.Test
import failgood.describe
import failgood.mock.mock
import kotlinx.coroutines.coroutineScope

@Test
object ContextVisitorTest {
    val tests =
        //        describe<ContextVisitor<*>> { // TODO find out why this no longer works.
        describe {
            it("can be easily created") {
                coroutineScope {
                    val staticConfig = StaticContextExecutionConfig<Unit>({}, this)
                    ContextVisitor<Unit, Unit>(
                        staticConfig = staticConfig,
                        contextStateCollector = ContextStateCollector<Unit>(staticConfig, false),
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
