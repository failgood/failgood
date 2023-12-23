package failgood.examples

import failgood.Test
import failgood.describe
import failgood.testsAbout
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class ReverserTest {
    val context =
        testsAbout(Reverser::class) {
            test("it can reverse palindromes") {
                expectThat(Reverser.reverse("racecar")).isEqualTo("racecar")
            }
        }
}
