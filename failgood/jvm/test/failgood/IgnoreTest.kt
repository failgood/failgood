package failgood

import java.time.Instant

@Test
object IgnoreTest {
    val test = describe<IsIgnored.Until> {
        it("is false before now") {
            assert(!IsIgnored.Until(Instant.now().minusSeconds(10)).isIgnored())
        }
        it("is true after now") {
            assert(IsIgnored.Until(Instant.now().plusSeconds(10)).isIgnored())
        }
    }
}
