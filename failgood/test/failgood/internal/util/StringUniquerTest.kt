package failgood.internal.util

import failgood.Test
import failgood.tests

@Test
class StringUniquerTest {
    val tests = tests {
        it("makes strings unique") {
            val s = StringUniquer()
            assert(s.makeUnique("failgood") == "failgood")
            assert(s.makeUnique("failgood") == "failgood-1")
        }
    }
}
