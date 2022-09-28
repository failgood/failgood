package failgood

@Test
object IgnoreTest {
    val test = describe<Ignored.Until> {
        it("is NotIgnored before now") {
            // we test that the date format is yyyy-mm-dd by using a day > 12
            assert(Ignored.Until("2020-01-20").isIgnored() == NotIgnored)
        }
        it("returns a reason after now") {
            assert(Ignored.Until("2024-01-20").isIgnored() == IgnoredBecause("2024-01-20 is after now"))
        }
    }
}
