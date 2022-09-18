package failgood.functional

import failgood.Test
import failgood.describe

@Test
class Test1 {
    val context = describe("wht") {
        dependency({ throw RuntimeException() })
    }
}
