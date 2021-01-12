package failfast.examples

import failfast.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

object ReverserTest {
    val context = describe(Reverser::class) {
        test("it can reverse palindromes") {
            expectThat(Reverser.reverse("racecar")).isEqualTo("racecar")
        }
    }
}
