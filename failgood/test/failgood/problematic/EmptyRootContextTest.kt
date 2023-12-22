package failgood.problematic

import failgood.Test
import failgood.describe
import failgood.tests

@Test
class EmptyRootContextTest {
    val context = tests("empty root context") {}
}
