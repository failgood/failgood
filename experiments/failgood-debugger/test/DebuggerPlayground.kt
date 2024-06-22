package failgood.experiments

import failgood.Test
import failgood.testCollection
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.assertNotNull

@Test
object DebuggerPlayground {
    val tests =
        testCollection("experimenting with debugging") {
            test("can start a class in a new vm and get variable values for every line") {
                val mainClass = Debuggee::class.java.name
                val variableInfo = runClass(mainClass)
                assertEquals(assertNotNull(variableInfo[10])["name"], "\"blubbi\"")
            }
        }
}
