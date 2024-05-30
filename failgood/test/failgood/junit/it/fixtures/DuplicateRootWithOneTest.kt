package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testsAbout

// this class registers two Test Collections with the same name. they should both appear in junit output
@TestFixture
class DuplicateRootWithOneTest {
    val context = listOf(testsAbout("name") { it("test") {} }, testsAbout("name") { it("test") {} })
}
