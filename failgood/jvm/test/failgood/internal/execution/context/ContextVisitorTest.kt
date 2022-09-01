package failgood.internal.execution.context

import failgood.Context
import failgood.RootContext
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
                    StaticContextExecutionConfig(RootContext {}, this),
                    mock(),
                    Context("root"),
                    runOnlyTag = null,
                    {},
                    mock(),
                    onlyRunSubcontexts = false
                )

            }
        }
    }
}
