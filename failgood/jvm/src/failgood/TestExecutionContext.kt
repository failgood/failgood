package failgood

/**
 * mutable data that is collected during test execution
 */
class TestExecutionContext {
    val events = mutableListOf<Event>()

    data class Event(val type: String, val body: String)
}
