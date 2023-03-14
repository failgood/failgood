package failgood.util

import failgood.Test
import failgood.describe
import kotlin.reflect.typeOf

@Test
object ToStringTest {
    val tests = describe("nice toString functions") {
        it("ktype has a niceString function") {
            assert(typeOf<Collection<String>>().niceString() == "Collection<String>")
            assert(typeOf<Collection<*>>().niceString() == "Collection<*>")
            assert(typeOf<String>().niceString() == "String")
        }
    }
}
