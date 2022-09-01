package failgood.internal.execution.context

import failgood.Context
import failgood.Test
import failgood.describe
import failgood.mock.mock

@Test
object ContextVisitorTest {
    val tests = describe<ContextVisitor<*>> {
        it("can be easily created") {
            ContextVisitor(mock(), Context("root"), mock(), given = {}, onlyRunSubcontexts = false)
        }
    }
}
