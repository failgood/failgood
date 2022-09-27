package failgood.junit.it.fixtures

import failgood.Ignored.Always
import failgood.describe
import failgood.internal.TestFixture

@TestFixture
object IgnoredTestFixture {
    private const val ROOT_CONTEXT_NAME = "the root context"
    val context = describe(ROOT_CONTEXT_NAME) {
        it("pending test", ignored = Always) {}
    }
}
