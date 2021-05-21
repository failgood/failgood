package failgood.examples

import failgood.describe
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Testable
class ReverserTest {
    val context = describe(Reverser::class) {
        test("it can reverse palindromes") {
            expectThat(Reverser.reverse("racecar")).isEqualTo("racecar")
        }
    }
}
