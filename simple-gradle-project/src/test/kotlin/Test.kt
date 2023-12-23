package failgood.gradle.test

import failgood.Test
import failgood.describe

@Test
class GradleTest {
    val tests = describe("running via gradle") {
        it("works") {}
    }
}
