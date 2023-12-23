package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testsAbout

// this used to return a cyclic graph
@TestFixture
class DuplicateRootWithOneTestFixture {
    val context = listOf(testsAbout("name") { it("test") {} }, testsAbout("name") { it("test") {} })
}
