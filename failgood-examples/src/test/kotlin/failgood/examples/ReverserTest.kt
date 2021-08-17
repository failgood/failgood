package failgood.examples

import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class ReverserTest {
    val context = describe(Reverser::class) {
        test("it can reverse palindromes") {
            expectThat(Reverser.reverse("racecar")).isEqualTo("racecar")
        }
    }
}
