@file:Suppress("KotlinConstantConditions")

package failgood.softly

import failgood.Test
import failgood.assert.containsExactlyInAnyOrder
import failgood.describe

@Test
object SoftAssertExample {
    val tests = describe("soft assert syntax") {
        it("could look like this") {
            val name = "klausi"
            softly {
                // standard boolean
                assert(name == "klausi")
                assert(name == "klausi") { "assert error message" }

                assert(!listOf("a", "b", "c").containsExactlyInAnyOrder("b", "a"))
            }
        }
    }
}
