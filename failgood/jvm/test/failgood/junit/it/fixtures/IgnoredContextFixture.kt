package failgood.junit.it.fixtures

import failgood.Ignored
import failgood.describe
import failgood.internal.TestFixture

@TestFixture
object IgnoredContextFixture {
    val context = describe("a context") {
        describe("a subcontext that is ignored", ignored = Ignored.Because("we are testing subcontext ignoring")) {
            it("contains tests that will not run") {
                throw RuntimeException("this should not run")
            }
        }
    }
}
