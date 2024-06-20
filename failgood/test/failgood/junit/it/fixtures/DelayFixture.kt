package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testCollection
import kotlinx.coroutines.delay

@TestFixture
class DelayFixture {
    val tests =
        testCollection("a context with slow tests") {
            test("that takes 2 seconds") { delay(2000) }
            test("another test that takes 2 seconds") { delay(2000) }
        }
}
