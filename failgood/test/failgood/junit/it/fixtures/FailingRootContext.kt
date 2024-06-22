package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testCollection

@TestFixture
class FailingRootContext {
    companion object {
        val thrownException = RuntimeException("root context failed")
    }

    // when the root context fails the other contexts should still work
    val context = testCollection("Failing Root Context") { throw thrownException }
}
