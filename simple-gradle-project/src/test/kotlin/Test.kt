package failgood.gradle.test

import failgood.Test
import failgood.describe

@Test
class GradleTest {
    val context = describe("running via gradle") {
        it("works") {}
    }
}
