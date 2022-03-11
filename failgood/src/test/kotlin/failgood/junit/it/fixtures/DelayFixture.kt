package failgood.junit.it.fixtures

import failgood.Test
import failgood.describe
import kotlinx.coroutines.delay

@Test
class DelayFixture {
    val context = describe("a context with slow tests") {
        test("that takes 2 seconds") {
            delay(2000)
        }
        test("another test that takes 2 seconds") {
            delay(2000)
        }
    }
}
