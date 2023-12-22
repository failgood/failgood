package failgood.util

import failgood.Test
import failgood.describe
import failgood.internal.util.niceString
import failgood.tests
import kotlin.reflect.typeOf

@Test
object ToStringTest {
    val tests =
        tests("nice toString functions") {
            it("ktype has a niceString function") {
                assert(typeOf<Collection<String>>().niceString() == "Collection<String>")
                assert(typeOf<Collection<*>>().niceString() == "Collection<*>")
                assert(typeOf<String>().niceString() == "String")
            }
        }
}
