package failgood.internal.given

import failgood.Test
import failgood.tests
import kotlin.test.assertEquals

@Test
object GivenDSLHandlerTest {
    val tests = tests {
        it("returns unit when there is no parent") {
            val rootHandler = RootGivenDSLHandler {}
            assertEquals(Unit, rootHandler.given())
        }
        it("can access the parent context") {
            val rootHandler = RootGivenDSLHandler {}
            val stringGiven = rootHandler.add { "stringGiven" }
            val childGiven = stringGiven.add { given() + " moreGiven" }
            val grandChildGiven = childGiven.add { given() + " and even more" }
            assertEquals("stringGiven moreGiven and even more", grandChildGiven.given())
        }
        it("can run a given that has a parent") {
            val handler = RootGivenDSLHandler { "my string" }
            assertEquals("my string", handler.given())
        }
    }
}
