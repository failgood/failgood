package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture
import failgood.tests

// this used to return a cyclic graph
@TestFixture
class DuplicateRootWithOneTestFixture {
    val context = listOf(tests("name") { it("test") {} }, tests("name") { it("test") {} })
}
