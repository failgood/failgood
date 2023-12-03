package failgood.internal.given

import failgood.Test
import failgood.describe
import kotlin.test.assertEquals

@Test
object GivenDSLHandlerTest {
    val tests = describe {
        it("can access the parent context") {
            val rootHandler = GivenDSLHandler<Unit>()
            val stringGiven = rootHandler.add { "stringGiven" }
            val childGiven = stringGiven.add { given() + " moreGiven" }
            assertEquals("stringGiven moreGiven", childGiven.given())
        }
    }
}
