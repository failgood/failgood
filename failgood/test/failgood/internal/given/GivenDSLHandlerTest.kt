package failgood.internal.given

import failgood.Test
import failgood.describe
import kotlin.test.assertEquals

@Test
object GivenDSLHandlerTest {
    val tests = describe {
        it("returns unit when there is no parent") {
            val rootHandler = GivenDSLHandler<Unit>()
            assertEquals(Unit, rootHandler.given())
        }
        it("can access the parent context") {
            val rootHandler = GivenDSLHandler<Unit>()
            val stringGiven = rootHandler.add { "stringGiven" }
            val childGiven = stringGiven.add { given() + " moreGiven" }
            val grandChildGiven = childGiven.add { given() + " and even more" }
            assertEquals("stringGiven moreGiven and even more", grandChildGiven.given())
        }
    }
}
