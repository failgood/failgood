package failgood.junit.it.fixtures

import failgood.Test
import failgood.describe

@Test
class FailingRootContext {
    companion object {
        val thrownException = RuntimeException("root context failed")
    }

    // when the root context fails the other contexts should still work
    @Suppress("unused")
    val context = describe("Failing Root Context") {
        throw thrownException
    }
}
