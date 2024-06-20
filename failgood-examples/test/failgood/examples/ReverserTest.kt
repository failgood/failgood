package failgood.examples

import failgood.Test
import failgood.testCollection
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class ReverserTest {
    val tests =
        testCollection(Reverser::class) {
            test("it can reverse palindromes") {
                expectThat(Reverser.reverse("racecar")).isEqualTo("racecar")
            }
        }
}
