package failgood

import failgood.experiments.until.Until

@Test
object IgnoreTest {
    val test = describe<Until> {
        it("is NotIgnored before now") {
            // we test that the date format is yyyy-mm-dd by using a day > 12
            assert(Until("2020-01-20").isIgnored() == null)
        }
        it("returns a reason after now") {
            assert(Until("2024-01-20").isIgnored() == "2024-01-20 is after now")
        }
    }
}
