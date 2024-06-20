package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testCollection

@TestFixture
class DoubleTestNamesInRootContextTestFixture {
    val tests =
        testCollection("failing tests") {
            it("test") {}
            it("test") {}
        }
}
