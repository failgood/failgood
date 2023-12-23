package failgood.util

import failgood.Test
import failgood.internal.util.niceString
import failgood.testsAbout
import kotlin.reflect.typeOf

@Test
object ToStringTest {
    val tests =
        testsAbout("nice toString functions") {
            it("ktype has a niceString function") {
                assert(typeOf<Collection<String>>().niceString() == "Collection<String>")
                assert(typeOf<Collection<*>>().niceString() == "Collection<*>")
                assert(typeOf<String>().niceString() == "String")
            }
        }
}
