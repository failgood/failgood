package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testsAbout

@TestFixture
class DoubleTestNamesInRootContextTestFixture {
    val tests =
        testsAbout("failing tests") {
            it("test") {}
            it("test") {}
        }
}
