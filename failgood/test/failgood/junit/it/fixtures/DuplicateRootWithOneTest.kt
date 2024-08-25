package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testCollection

// this class registers two Test Collections with the same name. they should both appear in junit
// output
@TestFixture
class DuplicateRootWithOneTest {
    val context =
        listOf(testCollection("name") { it("test") {} }, testCollection("name") { it("test") {} })
}
