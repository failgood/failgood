package failgood.softly

import org.opentest4j.MultipleFailuresError
import java.util.concurrent.CopyOnWriteArrayList

fun softly(function: AssertDSL.() -> Unit) {
    with(Asserter()) {
        function()
        check()
    }
}

interface AssertDSL {
    fun assert(b: Boolean)
    fun assert(b: Boolean, errorMessage: () -> String)
}

class Asserter : AssertDSL {
    private val errors = CopyOnWriteArrayList<java.lang.AssertionError>()
    override fun assert(b: Boolean) {
        assert(b) { "Assertion failed" }
    }

    override fun assert(b: Boolean, errorMessage: () -> String) {
        if (!b)
            errors.add(AssertionError(errorMessage()))
    }

    fun check() {
        if (errors.isNotEmpty())
            throw MultipleFailuresError("assertions failed", errors)
    }
}
