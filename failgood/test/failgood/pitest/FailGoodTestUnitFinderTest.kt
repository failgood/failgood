package failgood.pitest

import failgood.Ignored
import failgood.Test
import failgood.describe
import failgood.tests
import org.pitest.testapi.Description
import org.pitest.testapi.ResultCollector
import org.pitest.testapi.TestUnit
import org.pitest.testapi.TestUnitFinder
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.filter
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.single

// only compare the first 4 lines of the stacktrace because something is messing with stacktraces
fun throwableToString(t: Throwable) = t.stackTraceToString().lineSequence().take(4).joinToString()

val failure = AssertionError("failed")

object Tests {
    val context =
        tests("tests with different results") {
            test("failing test") { throw failure }
            test("pending test", ignored = Ignored.Because("testing ignored tests")) {}
            test("successful test") {}
        }
}

object NoTests {
    init {
        throw RuntimeException()
    }
}

@Test
class FailGoodTestUnitFinderTest {
    val context =
        describe(FailGoodTestUnitFinder::class) {
            it("creates a test unit for each test") {
                val finder: TestUnitFinder = FailGoodTestUnitFinder
                val testUnits = finder.findTestUnits(Tests::class.java, null)
                expectThat(testUnits).hasSize(3)
                val collector = TestResultCollector()
                testUnits.forEach { it.execute(collector) }
                val failedEvent =
                    Event(
                        Description(
                            "Tests: tests with different results > failing test",
                            Tests::class.java
                        ),
                        Type.END,
                        throwableToString(failure)
                    )
                expectThat(collector.events)
                    .filter { it.throwable != null }
                    .single()
                    .isEqualTo(failedEvent)
                expectThat(collector.events)
                    .containsExactlyInAnyOrder(
                        listOf(
                            Event(testUnits[0].description, Type.START, null),
                            Event(testUnits[1].description, Type.START, null),
                            Event(testUnits[2].description, Type.START, null),
                            failedEvent,
                            Event(
                                Description(
                                    "Tests: tests with different results > pending test",
                                    Tests::class.java
                                ),
                                Type.SKIPPED,
                                null
                            ),
                            Event(
                                Description(
                                    "Tests: tests with different results > successful test",
                                    Tests::class.java
                                ),
                                Type.END,
                                null
                            )
                        )
                    )
            }
            it("returns an no tests when the test class cannot be instantiated") {
                assert(
                    FailGoodTestUnitFinder.findTestUnits(NoTests::class.java, null) ==
                        listOf<TestUnit>()
                )
            }
        }

    private enum class Type {
        END,
        START,
        SKIPPED
    }

    private data class Event(val description: Description, val event: Type, val throwable: String?)

    private class TestResultCollector : ResultCollector {
        val events = mutableListOf<Event>()

        override fun notifyEnd(description: Description, t: Throwable) {
            events.add(Event(description, Type.END, throwableToString(t)))
        }

        override fun notifyEnd(description: Description) {
            events.add(Event(description, Type.END, null))
        }

        override fun notifyStart(description: Description) {
            events.add(Event(description, Type.START, null))
        }

        override fun notifySkipped(description: Description) {
            events.add(Event(description, Type.SKIPPED, null))
        }

        override fun shouldExit() = false
    }
}
