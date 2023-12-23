package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testsAbout

@TestFixture
class FailingRootContext {
    companion object {
        val thrownException = RuntimeException("root context failed")
    }

    // when the root context fails the other contexts should still work
    @Suppress("unused") val context = testsAbout("Failing Root Context") { throw thrownException }
}
