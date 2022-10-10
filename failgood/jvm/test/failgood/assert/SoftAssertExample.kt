@file:Suppress("KotlinConstantConditions")

package failgood.assert

import failgood.Test
import failgood.describe
import org.opentest4j.MultipleFailuresError
import java.util.concurrent.CopyOnWriteArrayList

@Test
object SoftAssertExample {
    val tests = describe("soft assert syntax") {
        it("could look like this") {
            val name = "klausi"
            assertSoftly {
                // standard boolean
                that(name == "klausi")
                that(name == "klausi") { "assert error message" }

                that(!listOf("a", "b", "c").containsExactlyInAnyOrder("b", "a"))
            }
        }
    }
}

private fun assertSoftly(function: AssertDSL.() -> Unit) {
    with(Asserter()) { function()
        check() }
}

interface AssertDSL {
    fun that(b: Boolean)
    fun that(b: Boolean, errorMessage: () -> String)
}

class Asserter : AssertDSL {
    private val errors = CopyOnWriteArrayList<java.lang.AssertionError>()
    override fun that(b: Boolean) {
        that(b) { "Assertion failed" }
    }

    override fun that(b: Boolean, errorMessage: () -> String) {
        if (!b)
            errors.add(AssertionError(errorMessage()))
    }

    fun check() {
        if (errors.isNotEmpty())
            throw MultipleFailuresError("assertions failed", errors)
    }
}
