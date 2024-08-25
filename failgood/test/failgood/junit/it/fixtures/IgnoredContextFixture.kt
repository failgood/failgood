package failgood.junit.it.fixtures

import failgood.Ignored
import failgood.internal.TestFixture
import failgood.testCollection

@TestFixture
object IgnoredContextFixture {
    val tests =
        testCollection("root context") {
            describe(
                "ignored context",
                ignored = Ignored.Because("we are testing subcontext ignoring")) {
                    it("contains tests that will not run") {
                        throw RuntimeException("this should not run")
                    }
                }
        }
}
