package failgood.internal.execution.context

import failgood.Context
import failgood.RootContext
import failgood.Test
import failgood.describe
import failgood.internal.ResourcesCloser
import kotlinx.coroutines.coroutineScope

@Test
object ContextVisitorTest {
    val tests = describe<ContextVisitor<*>> {
        it("can be easily created") {
            coroutineScope {
                ContextVisitor(
                    ContextExecutor(
                        RootContext("root") { }, this, false
                    ),
                    Context("root"), ResourcesCloser(this), given = {}
                )
            }
        }
    }
}
