package failgood.internal.util

import failgood.Test
import failgood.testCollection

@Test
class StringUniquerTest {
    val tests = testCollection {
        it("makes strings unique") {
            val s = StringUniquer()
            assert(s.makeUnique("failgood") == "failgood")
            assert(s.makeUnique("failgood") == "failgood-1")
        }
    }
}
