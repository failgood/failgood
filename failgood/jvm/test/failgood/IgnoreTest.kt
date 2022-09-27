package failgood

import java.time.Instant

@Test
object IgnoreTest {
    val test = describe<Ignored.Until> {
        describe("with an instant") {
            it("is false before now") {
                assert(!Ignored.Until(Instant.now().minusSeconds(10)).isIgnored())
            }
            it("is true after now") {
                assert(Ignored.Until(Instant.now().plusSeconds(10)).isIgnored())
            }
        }
        describe("with a String") {
            it("is false before now") {
                assert(!Ignored.Until("2020-01-20").isIgnored())
            }
            it("is true after now") {
                assert(Ignored.Until("2024-01-20").isIgnored())
            }
        }
    }
}
