package failgood.junit.it.fixtures

import failgood.Ignored
import failgood.describe
import failgood.internal.TestFixture

@TestFixture
object IgnoredContextFixture {
    val context = describe("root context") {
        describe("ignored context", ignored = Ignored.Because("we are testing subcontext ignoring")) {
            it("contains tests that will not run") {
                throw RuntimeException("this should not run")
            }
        }
    }
}
