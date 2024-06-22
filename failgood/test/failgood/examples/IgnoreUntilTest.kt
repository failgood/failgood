package failgood.examples

import failgood.Test
import failgood.testCollection

@Test
object IgnoreUntilTest {
    val test = testCollection {
        it("is NotIgnored before now") {
            // we test that the date format is yyyy-mm-dd by using a day > 12
            assert(Until("2020-01-20").isIgnored() == null)
        }
        it("returns a reason after now") {
            assert(Until("2029-01-20").isIgnored() == "2029-01-20 is after now")
        }
    }
}
