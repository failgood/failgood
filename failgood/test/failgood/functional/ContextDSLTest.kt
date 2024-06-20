package failgood.functional

import failgood.Suite
import failgood.Test
import failgood.describe
import failgood.testCollection
import kotlin.test.assertNotNull

@Test
object ContextDSLTest {
    val tests =
        testCollection("ContextDSL::describe") {
            it("works for class") {
                val e = NestedEvents()
                @Suppress("DEPRECATION") val results = Suite { describe<String> { it("test") { e.addEvent() } } }.run()
                val test = assertNotNull(results.allTests.singleOrNull())
                assert(test.test.context.name == "String")
                assert(e.globalEvents.isNotEmpty())
            }
        }
}
