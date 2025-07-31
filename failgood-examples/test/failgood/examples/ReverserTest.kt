package failgood.examples

import failgood.Test
import failgood.testCollection

@Test
class ReverserTest {
    val tests =
        testCollection(Reverser::class) {
            test("it can reverse palindromes") { assert(Reverser.reverse("racecar") == "racecar") }
        }
}
