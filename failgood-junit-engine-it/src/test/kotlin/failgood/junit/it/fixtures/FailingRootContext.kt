package failgood.junit.it.fixtures

import failgood.describe
import org.junit.platform.commons.annotation.Testable

@Testable
class FailingRootContext {
    // when the root context fails the other contexts should still work
    val context = describe("Failing Root Context") {
        throw RuntimeException()
    }
}
