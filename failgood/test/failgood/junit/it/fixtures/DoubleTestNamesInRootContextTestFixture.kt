package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture
import failgood.tests

@TestFixture
class DoubleTestNamesInRootContextTestFixture {
    val context =
        tests("failing tests") {
            it("test") {}
            it("test") {}
        }
}
