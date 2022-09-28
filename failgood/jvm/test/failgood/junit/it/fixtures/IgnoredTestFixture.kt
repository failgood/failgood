package failgood.junit.it.fixtures

import failgood.Ignored
import failgood.describe
import failgood.internal.TestFixture

@TestFixture
object IgnoredTestFixture {
    private const val ROOT_CONTEXT_NAME = "the root context"
    val context = describe(ROOT_CONTEXT_NAME) {
        it("pending test", ignored = Ignored.Because("Testing pending tests")) {}
    }
}
