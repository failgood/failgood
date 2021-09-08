package failgood.junit.it.fixtures

import failgood.Test
import failgood.describe

@Test
// returns a cyclic graph
class DuplicateRootWithOneTesd {
    val context = listOf(
        describe("name") {
            it("test") {}
        },
        describe("name") {
            it("test") {}
        }
    )
}
