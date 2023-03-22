package failgood

/**
 * mutable data that is collected during test execution
 */
class TestExecutionContext internal constructor(
    private val listener: ExecutionListener,
    private val testDescription: TestDescription
) {
    val events = mutableListOf<Event>()
    suspend fun event(type: String, body: String) {
        events.add(Event(type, body))
        listener.testEvent(testDescription, type, body.ifBlank { "<empty>" })
    }

    data class Event(val type: String, val body: String)
}
