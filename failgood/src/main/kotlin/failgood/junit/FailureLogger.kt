package failgood.junit

import failgood.FailGoodException

internal class FailureLogger(initial: Map<String, String> = mapOf()) {
    val map = initial.toMutableMap()
    fun unsafe(function: () -> Unit) {
        try {
            function()
        } catch (e: Exception) {
            fail(e)
        }
    }

    fun fail(e: Throwable) {
        throw FailGoodException(
            "=======\nAn Exception occurred inside Failgood.\n" + "if you run the latest version please submit a bug at " +
                    "https://github.com/failgood/failgood/issues " +
                    "or tell someone in the #failgood channel in the kotlin-lang slack.\n"+
                    "Please attach all this info including stacktraces. \n"+
            "message: ${e.message}\n here is what led to this: $map",
            e
        )
    }

    fun add(key: String, value: String) {
        map[key] = value
    }

}
