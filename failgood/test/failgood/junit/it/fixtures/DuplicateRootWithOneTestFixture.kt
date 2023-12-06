package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture

// this used to return a cyclic graph
@TestFixture
class DuplicateRootWithOneTestFixture {
    val context = listOf(describe("name") { it("test") {} }, describe("name") { it("test") {} })
}
