package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture
import failgood.tests

@TestFixture
class FailingRootContext {
    companion object {
        val thrownException = RuntimeException("root context failed")
    }

    // when the root context fails the other contexts should still work
    @Suppress("unused") val context = tests("Failing Root Context") { throw thrownException }
}
