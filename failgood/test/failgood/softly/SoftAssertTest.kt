@file:Suppress("KotlinConstantConditions")

package failgood.softly

import failgood.Test
import failgood.assert.containsExactlyInAnyOrder
import failgood.describe
import failgood.tests

@Test
object SoftAssertTest {
    val tests =
        tests("soft assert syntax") {
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
