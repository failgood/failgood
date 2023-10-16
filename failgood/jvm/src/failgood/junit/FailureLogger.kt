package failgood.junit

import failgood.FailGoodException

internal class FailureLogger {
    val map = mutableMapOf<String, String>()

    fun unsafe(function: () -> Unit) {
        try {
            function()
        } catch (e: Exception) {
            fail(e)
        }
    }

    fun fail(e: Throwable) {
        throw FailGoodException(
            "=======\nAn Exception occurred inside Failgood.\n" +
                "if you run the latest version please submit a bug at " +
                "https://github.com/failgood/failgood/issues " +
                "or tell someone in the #failgood channel in the kotlin-lang slack.\n" +
                "Please attach all this info including stacktraces. \n" +
                "message: ${e.message}\n Previously on failgood: $map",
            e
        )
    }

    fun add(key: String, value: String) {
        map[key] = value
    }

    fun envString(): String = map.toString()
}
