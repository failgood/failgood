package failgood.functional

import failgood.Suite
import failgood.Test
import failgood.describe
import kotlin.test.assertNotNull

@Test
object ContextDSLTest {
    val contextDescribeTests = describe("ContextDSL::describe") {
        it("works for class") {
            val e = NestedEvents()
            val results = Suite {
                describe<String> {
                    it("test") {
                        e.addEvent()
                    }
                }
            }.run()
            val test = assertNotNull(results.allTests.singleOrNull())
            assert(test.test.container.name == "String")
            assert(e.globalEvents.isNotEmpty())
        }
    }
}