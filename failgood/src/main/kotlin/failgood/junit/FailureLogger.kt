package failgood.junit

internal class FailureLogger(initial: Map<String, String>) {
    val map = initial.toMutableMap()
    fun unsafe(function: () -> Unit) {
        function()
    }

    fun add(key: String, value: String) {
        map[key] = value
    }

}
