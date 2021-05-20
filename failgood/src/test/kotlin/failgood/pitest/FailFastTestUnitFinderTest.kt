package failgood.pitest

import failgood.describe
import org.junit.platform.commons.annotation.Testable
import org.pitest.testapi.Description
import org.pitest.testapi.ResultCollector
import org.pitest.testapi.TestUnitFinder
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize

var failure: Throwable? = null

@Testable
class FailGoodTestUnitFinderTest {

    object Tests {
        val context = describe("tests with different results") {
            test("failing test") {
                failure = AssertionError("failed")
                throw failure!!
            }
            pending("pending test")
            test("successful test") {
            }
        }
    }

    val context =
        describe(FailGoodTestUnitFinder::class) {
            test("creates a test unit for each test") {
                val finder: TestUnitFinder = FailGoodTestUnitFinder
                val testUnits = finder.findTestUnits(Tests::class.java)
                expectThat(testUnits).hasSize(3)
                val collector = TestResultCollector()
                testUnits.forEach {
                    it.execute(collector)
                }
                expectThat(collector.events).containsExactlyInAnyOrder(
                    listOf(
                        Event(testUnits[0].description, Type.START, null),
                        Event(testUnits[1].description, Type.START, null),
                        Event(testUnits[2].description, Type.START, null),
                        Event(
                            Description("tests with different results > failing test", Tests::class.java),
                            Type.END,
                            failure
                        ),
                        Event(
                            Description("tests with different results > pending test", Tests::class.java),
                            Type.SKIPPED,
                            null
                        ),
                        Event(
                            Description("tests with different results > successful test", Tests::class.java),
                            Type.END,
                            null
                        )
                    )
                )
            }
        }

    private enum class Type {
        END, START, SKIPPED
    }

    private data class Event(val description: Description, val event: Type, val throwable: Throwable?)
    private class TestResultCollector : ResultCollector {
        val events = mutableListOf<Event>()
        override fun notifyEnd(description: Description, t: Throwable) {
            events.add(Event(description, Type.END, t))
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

