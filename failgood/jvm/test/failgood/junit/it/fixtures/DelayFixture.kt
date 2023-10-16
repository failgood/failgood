package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture
import kotlinx.coroutines.delay

@TestFixture
class DelayFixture {
    val context =
        describe("a context with slow tests") {
            test("that takes 2 seconds") { delay(2000) }
            test("another test that takes 2 seconds") { delay(2000) }
        }
}
